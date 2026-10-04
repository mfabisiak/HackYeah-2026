import { useEffect, useState } from 'react'
import { Alert, Anchor, Button, Group, Paper, Stack, Text, Title } from '@mantine/core'
import { IconAlertTriangle, IconFilePlus, IconInfoCircle } from '@tabler/icons-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { ApiClientError, formatApiError } from '../../api/errors'
import { unwrapApiResult } from '../../api/queryClient'
import { useAuth } from '../../auth/AuthContext'
import { ErrorAlert, LoadingState, PageHeader } from '../../components'
import { formatDate } from '../../components/dates'
import { ApplicationStatusBadge, CallStatusBadge } from './CallStatusBadge'
import { CanvasHint } from './CanvasHint'
import { IDEA_PARAM, callTimeLeft } from './CallsListPage'

/** Mirrors the server limits so the user learns about them before starting a third application. */
const MAX_SUBMITTED_PER_CALL = 2
const MAX_DRAFTS_PER_CALL = 5

export function CallDetailPage() {
  const { id = '' } = useParams()
  const [searchParams] = useSearchParams()
  const ideaId = searchParams.get(IDEA_PARAM)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { authenticated, ready, login } = useAuth()
  const [startError, setStartError] = useState<string | null>(null)

  const call = useQuery({
    queryKey: ['call', id],
    queryFn: () => unwrapApiResult(hubApi.calls.get(id)),
  })
  const mine = useQuery({
    queryKey: ['applications', 'mine', 'all'],
    queryFn: () => unwrapApiResult(hubApi.applications.mine(0, 100)),
    enabled: authenticated,
  })
  const idea = useQuery({
    queryKey: ['idea', ideaId],
    queryFn: () => unwrapApiResult(hubApi.ideas.get(ideaId ?? '')),
    enabled: authenticated && ideaId !== null,
  })

  useEffect(() => {
    document.title = `${call.data?.title ?? 'Nabór'} | HubMI`
  }, [call.data?.title])

  const start = useMutation({
    mutationFn: () => unwrapApiResult(hubApi.calls.apply(id, ideaId)),
    onSuccess: (application) => {
      void queryClient.invalidateQueries({ queryKey: ['applications', 'mine'] })
      navigate(`/wnioski/${application.id}`)
    },
    onError: (error) => {
      if (error instanceof ApiClientError && error.status === 401) login()
      else setStartError(formatApiError(error))
    },
  })

  if (call.isPending) return <LoadingState message="Wczytywanie naboru..." />
  if (call.isError || !call.data) {
    const notFound = call.error instanceof ApiClientError && call.error.status === 404
    return (
      <Stack>
        <PageHeader title="Nabór" breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Nabory', href: '/nabory' }, { title: 'Nabór' }]} />
        <ErrorAlert
          title={notFound ? 'Nie znaleziono naboru' : 'Nie udało się wczytać naboru'}
          message={notFound ? 'Ten nabór nie istnieje.' : formatApiError(call.error)}
          onRetry={notFound ? undefined : () => void call.refetch()}
        />
      </Stack>
    )
  }

  const data = call.data
  const inThisCall = (mine.data?.items ?? []).filter((a) => a.callId === data.id)
  const submitted = inThisCall.filter((a) => a.status === 'SUBMITTED')
  const drafts = inThisCall.filter((a) => a.status === 'DRAFT')
  const timeLeft = callTimeLeft(data)

  const blockedReason =
    submitted.length >= MAX_SUBMITTED_PER_CALL
      ? `Złożono już ${MAX_SUBMITTED_PER_CALL} wnioski w tym naborze – to maksymalna liczba. Nie można rozpocząć kolejnego wniosku.`
      : drafts.length >= MAX_DRAFTS_PER_CALL
        ? `Masz już ${MAX_DRAFTS_PER_CALL} szkiców wniosków w tym naborze – to maksymalna liczba. Dokończ i złóż któryś z nich.`
        : null

  return (
    <Stack gap="lg" maw={820}>
      <PageHeader
        title={data.title}
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Nabory', href: '/nabory' }, { title: 'Nabór' }]}
      />

      <Paper withBorder p="lg" radius="md">
        <Stack gap="sm">
          <Group>
            <CallStatusBadge status={data.status} />
          </Group>
          <Text>
            Termin naboru: od <time dateTime={data.opensAt}>{formatDate(data.opensAt)}</time> do{' '}
            <time dateTime={data.closesAt}>{formatDate(data.closesAt)}</time>
          </Text>
          {timeLeft && <Text fw={700}>{timeLeft}</Text>}
          <Text style={{ whiteSpace: 'pre-wrap' }}>{data.description}</Text>
        </Stack>
      </Paper>

      <Paper withBorder p="lg" radius="md" component="section" aria-labelledby="apply-heading">
        <Stack gap="md" align="flex-start">
          <Title order={2} size="h3" id="apply-heading">
            Złóż wniosek
          </Title>

          {data.status === 'UPCOMING' && (
            <Alert color="blue" variant="light" role="note" icon={<IconInfoCircle size={22} aria-hidden="true" />}>
              Nabór jeszcze się nie rozpoczął. Wniosek będzie można złożyć od{' '}
              <time dateTime={data.opensAt}>{formatDate(data.opensAt)}</time>.
            </Alert>
          )}
          {data.status === 'CLOSED' && (
            <Alert color="gray" variant="light" role="note" icon={<IconInfoCircle size={22} aria-hidden="true" />}>
              Nabór zakończył się <time dateTime={data.closesAt}>{formatDate(data.closesAt)}</time>. Wnioski nie są już
              przyjmowane.
            </Alert>
          )}

          {data.status === 'OPEN' && ready && !authenticated && (
            <>
              <Text>Aby złożyć wniosek, zaloguj się. Po zalogowaniu wrócisz na tę stronę.</Text>
              <Button onClick={login}>Zaloguj się, aby złożyć wniosek</Button>
            </>
          )}

          {data.status === 'OPEN' && authenticated && (
            <>
              {idea.data && (
                <Alert color="blue" variant="light" role="note" icon={<IconInfoCircle size={22} aria-hidden="true" />}>
                  Wniosek zostanie wstępnie wypełniony danymi Twojego pomysłu „{idea.data.title}”.
                </Alert>
              )}
              {submitted.length > 0 && submitted.length < MAX_SUBMITTED_PER_CALL && (
                <Text>
                  Złożono już {submitted.length} wniosek w tym naborze. W jednym naborze można złożyć najwyżej{' '}
                  {MAX_SUBMITTED_PER_CALL} wnioski.
                </Text>
              )}
              {blockedReason ? (
                <Alert color="yellow" role="note" icon={<IconAlertTriangle size={22} aria-hidden="true" />} title="Nie można rozpocząć nowego wniosku">
                  {blockedReason}
                </Alert>
              ) : (
                <Button
                  size="lg"
                  leftSection={<IconFilePlus size={20} aria-hidden="true" />}
                  onClick={() => {
                    setStartError(null)
                    start.mutate()
                  }}
                  loading={start.isPending}
                >
                  Rozpocznij nowy wniosek
                </Button>
              )}
              {startError && (
                <Alert color="red" icon={<IconAlertTriangle size={22} aria-hidden="true" />} role="alert" title="Nie udało się rozpocząć wniosku">
                  {startError}
                </Alert>
              )}
            </>
          )}

          {inThisCall.length > 0 && (
            <Stack gap="xs" w="100%">
              <Title order={3} size="h4">
                Twoje wnioski w tym naborze
              </Title>
              <Stack component="ul" gap="xs" p={0} m={0} style={{ listStyle: 'none' }}>
                {inThisCall.map((application) => (
                  <li key={application.id}>
                    <Group gap="sm">
                      <ApplicationStatusBadge status={application.status} />
                      <Anchor component={Link} to={`/wnioski/${application.id}`}>
                        {application.status === 'DRAFT' ? 'Kontynuuj wypełnianie' : 'Zobacz wniosek'}
                        {application.title ? `: ${application.title}` : ''}
                      </Anchor>
                    </Group>
                  </li>
                ))}
              </Stack>
            </Stack>
          )}
        </Stack>
      </Paper>

      {data.status !== 'CLOSED' && <CanvasHint />}
    </Stack>
  )
}
