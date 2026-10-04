import { useEffect } from 'react'
import { Anchor, Button, Card, Group, Stack, Text, Title } from '@mantine/core'
import { IconArrowRight, IconCalendarEvent } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import type { GrantCallJs } from 'hubmi-client'
import { Link, useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { formatApiError } from '../../api/errors'
import { unwrapApiResult } from '../../api/queryClient'
import { EmptyState, ErrorAlert, LoadingState, PageHeader } from '../../components'
import { daysUntil, formatDate } from '../../components/dates'
import { CallStatusBadge } from './CallStatusBadge'

export const IDEA_PARAM = 'pomysl'

const GROUPS = [
  { status: 'OPEN', title: 'Trwające nabory' },
  { status: 'UPCOMING', title: 'Nadchodzące nabory' },
  { status: 'CLOSED', title: 'Zakończone nabory' },
] as const

/** Plain-language remaining time, e.g. “Do końca naboru zostało 12 dni”. */
export function callTimeLeft(call: GrantCallJs, now: Date = new Date()): string | null {
  const days = daysUntil(call.closesAt, now)
  if (call.status !== 'OPEN') return null
  if (days <= 0) return 'Nabór kończy się dzisiaj'
  return days === 1 ? 'Do końca naboru został 1 dzień' : `Do końca naboru zostało ${days} dni`
}

export function CallsListPage() {
  const [searchParams] = useSearchParams()
  const ideaId = searchParams.get(IDEA_PARAM)
  const calls = useQuery({
    queryKey: ['calls'],
    queryFn: () => unwrapApiResult(hubApi.calls.list()),
  })

  useEffect(() => {
    document.title = 'Nabory na mikrogranty | HubMI'
  }, [])

  const suffix = ideaId ? `?${IDEA_PARAM}=${encodeURIComponent(ideaId)}` : ''
  const items = calls.data ?? []

  return (
    <Stack gap="lg">
      <PageHeader
        title="Nabory na mikrogranty"
        subtitle="Wybierz nabór, w którym chcesz złożyć wniosek. Formularz wniosku poprowadzi Cię krok po kroku."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Nabory' }]}
      />
      {ideaId && (
        <Text role="status">Wybierz nabór, a wniosek zostanie wstępnie wypełniony danymi Twojego pomysłu.</Text>
      )}

      {calls.isPending && <LoadingState message="Wczytywanie naborów..." />}
      {calls.isError && (
        <ErrorAlert title="Nie udało się wczytać naborów" message={formatApiError(calls.error)} onRetry={() => void calls.refetch()} />
      )}
      {calls.isSuccess && items.length === 0 && (
        <EmptyState
          icon={<IconCalendarEvent size={32} />}
          title="Nie ma jeszcze żadnych naborów"
          description="Gdy ROPS ogłosi nabór, pojawi się tutaj."
        />
      )}

      {GROUPS.map((group) => {
        const inGroup = items.filter((call) => call.status === group.status)
        if (inGroup.length === 0) return null
        return (
          <section key={group.status} aria-labelledby={`calls-${group.status}`}>
            <Title order={2} size="h3" id={`calls-${group.status}`} mb="md">
              {group.title}
            </Title>
            <Stack component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }}>
              {inGroup.map((call) => (
                <li key={call.id}>
                  <Card withBorder padding="lg" radius="md">
                    <Stack gap="sm">
                      <Group justify="space-between" align="flex-start" gap="sm">
                        <Title order={3} size="h4">
                          <Anchor component={Link} to={`/nabory/${call.id}${suffix}`} underline="hover" c="inherit">
                            {call.title}
                          </Anchor>
                        </Title>
                        <CallStatusBadge status={call.status} />
                      </Group>
                      <Text>
                        Termin: od <time dateTime={call.opensAt}>{formatDate(call.opensAt)}</time> do{' '}
                        <time dateTime={call.closesAt}>{formatDate(call.closesAt)}</time>
                      </Text>
                      {callTimeLeft(call) && <Text fw={600}>{callTimeLeft(call)}</Text>}
                      <Group>
                        <Button
                          component={Link}
                          to={`/nabory/${call.id}${suffix}`}
                          variant={call.status === 'OPEN' ? 'filled' : 'light'}
                          rightSection={<IconArrowRight size={18} aria-hidden="true" />}
                        >
                          {call.status === 'OPEN' ? 'Zobacz nabór i złóż wniosek' : 'Zobacz szczegóły naboru'}
                        </Button>
                      </Group>
                    </Stack>
                  </Card>
                </li>
              ))}
            </Stack>
          </section>
        )
      })}
    </Stack>
  )
}
