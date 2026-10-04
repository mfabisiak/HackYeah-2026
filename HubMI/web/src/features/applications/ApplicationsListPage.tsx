import { useEffect, useMemo, useState } from 'react'
import { Anchor, Button, Card, Group, Stack, Text, Title } from '@mantine/core'
import { IconFileText } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { formatApiError } from '../../api/errors'
import { unwrapApiResult } from '../../api/queryClient'
import { AccessiblePagination, EmptyState, ErrorAlert, LoadingState, PageHeader } from '../../components'
import { formatDate, formatDateTime } from '../../components/dates'
import { ApplicationStatusBadge } from './CallStatusBadge'

const PAGE_SIZE = 10

export function ApplicationsListPage() {
  const [page, setPage] = useState(0)
  const applications = useQuery({
    queryKey: ['applications', 'mine', page],
    queryFn: () => unwrapApiResult(hubApi.applications.mine(page, PAGE_SIZE)),
    refetchOnMount: 'always',
  })
  const calls = useQuery({
    queryKey: ['calls'],
    queryFn: () => unwrapApiResult(hubApi.calls.list()),
  })
  const callTitles = useMemo(() => new Map((calls.data ?? []).map((call) => [call.id, call.title])), [calls.data])

  useEffect(() => {
    document.title = 'Moje wnioski | HubMI'
  }, [])

  const total = applications.data?.total ?? 0

  return (
    <Stack gap="lg">
      <PageHeader
        title="Moje wnioski"
        subtitle="Szkice i złożone wnioski o mikrogrant. Szkic możesz dokończyć w dowolnym momencie."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje wnioski' }]}
        actions={
          <Button component={Link} to="/nabory">
            Zobacz nabory
          </Button>
        }
      />

      {applications.isPending && <LoadingState message="Wczytywanie Twoich wniosków..." />}
      {applications.isError && (
        <ErrorAlert
          title="Nie udało się wczytać wniosków"
          message={formatApiError(applications.error)}
          onRetry={() => void applications.refetch()}
        />
      )}

      {applications.isSuccess && (
        <>
          <Text role="status">{total === 0 ? 'Nie masz jeszcze żadnych wniosków.' : `Liczba Twoich wniosków: ${total}.`}</Text>
          {total === 0 ? (
            <EmptyState
              icon={<IconFileText size={32} />}
              title="Nie masz jeszcze żadnych wniosków"
              description="Wybierz nabór i rozpocznij wniosek. Szkic zapisuje się automatycznie."
              action={
                <Button component={Link} to="/nabory">
                  Wybierz nabór
                </Button>
              }
            />
          ) : (
            <Stack component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }} aria-label="Lista Twoich wniosków">
              {applications.data.items.map((application) => (
                <li key={application.id}>
                  <Card withBorder padding="lg" radius="md">
                    <Stack gap="sm">
                      <Group justify="space-between" align="flex-start" gap="sm">
                        <Title order={2} size="h4">
                          <Anchor component={Link} to={`/wnioski/${application.id}`} underline="hover" c="inherit">
                            {application.title || 'Wniosek bez tytułu'}
                          </Anchor>
                        </Title>
                        <ApplicationStatusBadge status={application.status} />
                      </Group>
                      <Text c="dimmed" size="sm">
                        Nabór: {callTitles.get(application.callId) ?? 'nabór'} ·{' '}
                        {application.submittedAt ? (
                          <>
                            Złożono: <time dateTime={application.submittedAt}>{formatDateTime(application.submittedAt)}</time>
                          </>
                        ) : (
                          <>
                            Ostatnia zmiana: <time dateTime={application.updatedAt}>{formatDate(application.updatedAt)}</time>
                          </>
                        )}
                      </Text>
                      <Group>
                        <Button component={Link} to={`/wnioski/${application.id}`} variant={application.status === 'DRAFT' ? 'filled' : 'light'} size="sm">
                          {application.status === 'DRAFT' ? 'Kontynuuj wypełnianie' : 'Zobacz wniosek'}
                        </Button>
                      </Group>
                    </Stack>
                  </Card>
                </li>
              ))}
            </Stack>
          )}
          <AccessiblePagination
            total={Math.max(1, Math.ceil(total / PAGE_SIZE))}
            value={page + 1}
            onChange={(next) => setPage(next - 1)}
            totalCount={total}
            itemsPerPage={PAGE_SIZE}
            ariaLabel="Paginacja listy wniosków"
          />
        </>
      )}
    </Stack>
  )
}
