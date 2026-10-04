import {
  Badge,
  Button,
  Card,
  Group,
  Pagination,
  Paper,
  Select,
  Stack,
  Table,
  Text,
} from '@mantine/core'
import {
  IconArrowRight,
  IconClock,
  IconMessageCircle,
  IconMessagePlus,
  IconRefresh,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toFriendlyErrorMessage } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import type { PageJs, ThreadJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function ExpertConsultationsQueue() {
  const [data, setData] = useState<PageJs<ThreadJs> | null>(null)
  const [filterUnread, setFilterUnread] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  useEffect(() => {
    let cancelled = false

    const fetchThreads = async () => {
      try {
        const res = await hubApi.threads.list(currentPage - 1, PAGE_SIZE)
        if (cancelled) return
        if (res.error) {
          setError(toFriendlyErrorMessage(res.error, 'Nie udało się pobrać listy wątków konsultacyjnych.'))
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyErrorMessage(err, 'Wystąpił błąd podczas ładowania wątków.'))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchThreads()

    return () => {
      cancelled = true
    }
  }, [currentPage, refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setRefreshTrigger((prev) => prev + 1)
  }

  const allThreads = data?.items ?? []
  const filteredThreads = allThreads.filter((t) => {
    if (filterUnread === 'UNREAD') return t.unread
    return true
  })

  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  return (
    <Stack gap="lg">
      <Paper withBorder p="md" radius="md">
        <Group justify="space-between" wrap="wrap" gap="md">
          <Group gap="md">
            <Select
              label="Filtruj wątki"
              aria-label="Filtruj wątki"
              value={filterUnread}
              onChange={(val) => setFilterUnread(val || 'ALL')}
              data={[
                { value: 'ALL', label: 'Wszystkie wątki' },
                { value: 'UNREAD', label: 'Tylko nieprzeczytane / nowe' },
              ]}
              w={240}
              size="sm"
            />
          </Group>
          <Group gap="xs">
            <Button
              variant="default"
              size="sm"
              leftSection={<IconRefresh size={16} aria-hidden="true" />}
              onClick={handleRefresh}
            >
              Odśwież
            </Button>
            <Button
              component={Link}
              to="/wiadomosci"
              size="sm"
              color="indigo"
              leftSection={<IconMessagePlus size={16} aria-hidden="true" />}
            >
              Nowa wiadomość
            </Button>
          </Group>
        </Group>
      </Paper>

      {isLoading ? (
        <LoadingState message="Wczytuję wątki konsultacyjne..." minHeight={200} />
      ) : error ? (
        <ErrorAlert message={error} onRetry={handleRefresh} />
      ) : filteredThreads.length === 0 ? (
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Stack align="center" gap="sm">
            <IconMessageCircle size={40} color="var(--mantine-color-dimmed)" aria-hidden="true" />
            <Text size="md" c="dimmed">
              Brak aktywnych wątków konsultacyjnych spełniających kryteria.
            </Text>
            <Button component={Link} to="/wiadomosci" variant="light" color="blue" size="sm">
              Przejdź do skrzynki wiadomości
            </Button>
          </Stack>
        </Card>
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="md">
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700 }}>
                  Temat rozmowy
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Ostatnia aktywność
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Status
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Akcja
                </Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {filteredThreads.map((thread) => {
                const formattedDate = new Date(thread.lastMessageAt).toLocaleString('pl-PL', {
                  day: 'numeric',
                  month: 'short',
                  hour: '2-digit',
                  minute: '2-digit',
                })

                return (
                  <Table.Tr key={thread.id}>
                    <Table.Td style={{ minWidth: 260 }}>
                      <Text fw={thread.unread ? 800 : 600} style={{ fontSize: '1.05rem' }}>
                        {thread.subject}
                      </Text>
                      {thread.relatedIdeaId && (
                        <Text size="xs" c="dimmed">
                          Powiązany pomysł ID: {thread.relatedIdeaId}
                        </Text>
                      )}
                    </Table.Td>

                    <Table.Td style={{ minWidth: 160, textAlign: 'center' }}>
                      <Group gap={6} justify="center">
                        <IconClock size={16} color="gray" aria-hidden="true" />
                        <Text size="sm">{formattedDate}</Text>
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 140, textAlign: 'center' }}>
                      <Group justify="center">
                        {thread.unread ? (
                          <Badge color="red" variant="filled" size="sm">
                            Nowa wiadomość
                          </Badge>
                        ) : (
                          <Badge color="gray" variant="light" size="sm">
                            Przeczytane
                          </Badge>
                        )}
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ textAlign: 'center', minWidth: 160 }}>
                      <Group justify="center">
                        <Button
                          component={Link}
                          to={`/wiadomosci/${thread.id}`}
                          size="sm"
                          variant="light"
                          color="indigo"
                          rightSection={<IconArrowRight size={16} aria-hidden="true" />}
                          styles={{ root: { fontWeight: 600 } }}
                        >
                          Otwórz czat
                        </Button>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                )
              })}
            </Table.Tbody>
          </Table>
        </Card>
      )}

      {totalPages > 1 && (
        <Group justify="center" mt="md">
          <Pagination
            value={currentPage}
            onChange={setCurrentPage}
            total={totalPages}
            aria-label="Nawigacja po stronach listy wątków"
            size="md"
          />
        </Group>
      )}
    </Stack>
  )
}
