import { useEffect } from 'react'
import { Alert, Button, Group, Paper, Stack, Text, Title } from '@mantine/core'
import { IconArrowRight, IconCircleCheck } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { ApiClientError, formatApiError } from '../../api/errors'
import { unwrapApiResult } from '../../api/queryClient'
import { ErrorAlert, LoadingState, PageHeader } from '../../components'
import { formatDate } from '../../components/dates'
import { IDEA_STATUS_DESCRIPTIONS, INNOVATION_STAGE_NAMES, TARGET_GROUP_NAMES } from './constants'
import { IdeaStatusBadge } from './IdeaStatusBadge'

export function IdeaDetailPage() {
  const { id = '' } = useParams()
  const location = useLocation()
  const justCreated = (location.state as { justCreated?: boolean } | null)?.justCreated === true
  const idea = useQuery({
    queryKey: ['idea', id],
    queryFn: () => unwrapApiResult(hubApi.ideas.get(id)),
  })

  // The idea's title is the author's own text; keep it out of the tab title and browser history.
  useEffect(() => {
    document.title = 'Szczegóły pomysłu | HubMI'
  }, [])

  if (idea.isPending) return <LoadingState message="Wczytywanie pomysłu..." />
  if (idea.isError || !idea.data) {
    const notFound = idea.error instanceof ApiClientError && idea.error.status === 404
    return (
      <Stack>
        <PageHeader title="Pomysł" breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje pomysły', href: '/pomysly' }, { title: 'Pomysł' }]} />
        <ErrorAlert
          title={notFound ? 'Nie znaleziono pomysłu' : 'Nie udało się wczytać pomysłu'}
          message={notFound ? 'Ten pomysł nie istnieje albo nie masz do niego dostępu.' : formatApiError(idea.error)}
          onRetry={notFound ? undefined : () => void idea.refetch()}
        />
      </Stack>
    )
  }

  const data = idea.data
  return (
    <Stack gap="lg" maw={820}>
      <PageHeader
        title={data.title}
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje pomysły', href: '/pomysly' }, { title: 'Pomysł' }]}
      />

      {justCreated && (
        <Alert color="teal" variant="light" icon={<IconCircleCheck size={24} aria-hidden="true" />} title="Pomysł został zgłoszony" role="status">
          Dziękujemy! Pracownicy ROPS zapoznają się z pomysłem. Zmiany statusu zobaczysz na tej stronie i w „Moich pomysłach”.
        </Alert>
      )}

      <Paper withBorder p="lg" radius="md">
        <Stack gap="sm">
          <Title order={2} size="h4">
            Co dzieje się z pomysłem
          </Title>
          <Group>
            <IdeaStatusBadge status={data.status} />
          </Group>
          <Text>{IDEA_STATUS_DESCRIPTIONS[data.status]}</Text>
          {data.adminComment && (
            <Text>
              <strong>Komentarz ROPS:</strong> {data.adminComment}
            </Text>
          )}
          <Text size="sm" c="dimmed">
            Zgłoszono: <time dateTime={data.createdAt}>{formatDate(data.createdAt)}</time>
          </Text>
        </Stack>
      </Paper>

      <Paper withBorder p="lg" radius="md">
        <Title order={2} size="h4" mb="sm">
          Fiszka pomysłu
        </Title>
        <Stack component="dl" gap="md" m={0}>
          <div>
            <Text component="dt" fw={700}>
              Istota pomysłu
            </Text>
            <Text component="dd" m={0} style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
              {data.essence}
            </Text>
          </div>
          <div>
            <Text component="dt" fw={700}>
              Dla kogo
            </Text>
            <Text component="dd" m={0}>
              {data.targetGroups.map((g) => TARGET_GROUP_NAMES[g] ?? g).join(', ')}
            </Text>
          </div>
          <div>
            <Text component="dt" fw={700}>
              Etap
            </Text>
            <Text component="dd" m={0}>
              {INNOVATION_STAGE_NAMES[data.stage] ?? data.stage}
            </Text>
          </div>
        </Stack>
      </Paper>

      <Paper withBorder p="lg" radius="md">
        <Stack gap="sm" align="flex-start">
          <Title order={2} size="h4">
            Co dalej?
          </Title>
          <Text>
            Gdy trwa nabór na mikrogranty, możesz przygotować wniosek na podstawie tego pomysłu – tytuł i opis wpiszemy za
            Ciebie.
          </Text>
          <Button component={Link} to={`/nabory?pomysl=${data.id}`} rightSection={<IconArrowRight size={18} aria-hidden="true" />}>
            Przygotuj wniosek na podstawie pomysłu
          </Button>
        </Stack>
      </Paper>
    </Stack>
  )
}
