import {
  Badge,
  Button,
  Card,
  Group,
  Paper,
  SegmentedControl,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconArrowRight,
  IconCheck,
  IconClock,
  IconHelpCircle,
  IconInbox,
  IconMessagePlus,
  IconRefresh,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { formatPolishDateTime, formatRelativePolishTime } from './constants'
import { NewThreadModal } from './NewThreadModal'
import type { ThreadJs } from 'hubmi-client'

export function ThreadsListPage() {
  const { hasRole } = useAuth()
  const [searchParams] = useSearchParams()
  const isAdmin = hasRole('admin')

  const [threads, setThreads] = useState<ThreadJs[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filterMode, setFilterMode] = useState<'all' | 'unread'>('all')
  const [newModalOpened, setNewModalOpened] = useState(
    searchParams.get('nowy') === '1',
  )
  const initialIdeaId = searchParams.get('pomysl') || undefined

  const [refreshTrigger, setRefreshTrigger] = useState(0)

  useEffect(() => {
    document.title = 'Wiadomości i kontakt z ROPS | HubMI'
    let cancelled = false

    const load = async () => {
      try {
        const res = await hubApi.threads.list(0, 50)
        if (cancelled) return
        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać listy rozmów.')
        } else if (res.value) {
          setThreads(res.value.items)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd podczas połączenia z serwerem wiadomości.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void load()

    return () => {
      cancelled = true
    }
  }, [refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setRefreshTrigger((prev) => prev + 1)
  }

  const filteredThreads = threads.filter((t) => {
    if (filterMode === 'unread') {
      return t.unread
    }
    return true
  })

  return (
    <Stack gap="xl">
      <PageHeader
        title="Wiadomości i kontakt z ROPS"
        subtitle="Bezpośredni dialog z pracownikami Regionalnego Ośrodka Polityki Społecznej w Krakowie oraz ekspertami."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Wiadomości' },
        ]}
      />

      {/* Senior assistance informational panel */}
      <Paper
        withBorder
        p="lg"
        radius="md"
        style={{
          backgroundColor: 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))',
          borderLeft: '5px solid var(--mantine-color-blue-filled)',
        }}
      >
        <Group align="flex-start" gap="md">
          <ThemeIcon size={38} radius="xl" color="blue" variant="light">
            <IconHelpCircle size={22} aria-hidden="true" />
          </ThemeIcon>
          <Stack gap={4} style={{ flex: 1 }}>
            <Title order={2} size="h4" style={{ fontSize: '1.2rem', fontWeight: 700 }}>
              Jak działa skrzynka wiadomości?
            </Title>
            <Text size="md" style={{ fontSize: '1.1rem', lineHeight: 1.6 }}>
              W tym miejscu prowadzisz prywatne rozmowy z koordynatorami Regionalnego Ośrodka
              Polityki Społecznej. Możesz zadać pytanie, skonsultować pomysł na innowację lub
              dopytać o szczegóły naboru wniosków. Odpowiedź pojawi się bezpośrednio w danym wątku
              oraz w Twoich powiadomieniach.
            </Text>
          </Stack>
        </Group>
      </Paper>

      {/* Actions toolbar */}
      <Group justify="space-between" align="center" wrap="wrap" gap="md">
        <Button
          onClick={() => setNewModalOpened(true)}
          size="lg"
          color="blue"
          leftSection={<IconMessagePlus size={22} aria-hidden="true" />}
          styles={{ root: { fontSize: '1.1rem', fontWeight: 600, height: 48 } }}
        >
          Napisz nową wiadomość do ROPS
        </Button>

        <Group gap="sm" wrap="wrap">
          {isAdmin && (
            <SegmentedControl
              value={filterMode}
              onChange={(val) => setFilterMode(val as 'all' | 'unread')}
              data={[
                { label: 'Wszystkie rozmowy', value: 'all' },
                { label: 'Wymaga odpowiedzi (nowe)', value: 'unread' },
              ]}
              size="md"
              styles={{
                label: { fontSize: '1rem', fontWeight: 600, padding: '8px 16px' },
              }}
            />
          )}

          <Button
            variant="default"
            size="lg"
            onClick={handleRefresh}
            leftSection={<IconRefresh size={18} aria-hidden="true" />}
            styles={{ root: { fontSize: '1rem', height: 48 } }}
          >
            Odśwież listę
          </Button>
        </Group>
      </Group>

      {error && <ErrorAlert message={error} />}

      {isLoading ? (
        <LoadingState message="Wczytuję listę wiadomości..." minHeight={300} />
      ) : filteredThreads.length === 0 ? (
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Stack align="center" gap="md" py="xl">
            <ThemeIcon size={64} radius="xl" color="gray" variant="light">
              <IconInbox size={36} aria-hidden="true" />
            </ThemeIcon>
            <Title order={3} size="h3" style={{ fontSize: '1.3rem' }}>
              {filterMode === 'unread'
                ? 'Brak wiadomości wymagających odpowiedzi'
                : 'Nie masz jeszcze żadnych rozpoczętych rozmów'}
            </Title>
            <Text size="lg" c="dimmed" style={{ maxWidth: 600, fontSize: '1.1rem', lineHeight: 1.6 }}>
              {filterMode === 'unread'
                ? 'Wszystkie wątki są aktualnie przeczytane i obsłużone.'
                : 'Jeśli masz pytanie dotyczące innowacji, naboru lub potrzebujesz porady, kliknij poniższy przycisk, aby napisać pierwszą wiadomość do pracowników ROPS.'}
            </Text>
            {filterMode === 'all' && (
              <Button
                onClick={() => setNewModalOpened(true)}
                size="lg"
                color="blue"
                leftSection={<IconMessagePlus size={22} aria-hidden="true" />}
                styles={{ root: { fontSize: '1.05rem', fontWeight: 600, marginTop: 8 } }}
              >
                Napisz pierwszą wiadomość
              </Button>
            )}
          </Stack>
        </Card>
      ) : (
        <Stack
          component="ul"
          gap="md"
          p={0}
          m={0}
          style={{ listStyle: 'none' }}
          aria-label="Lista rozmów z ROPS"
        >
          {filteredThreads.map((thread) => {
            return (
              <li key={thread.id}>
                <Card
                  withBorder
                  padding="lg"
                  radius="md"
                  component={Link}
                  to={`/wiadomosci/${thread.id}`}
                  style={{
                    textDecoration: 'none',
                    color: 'inherit',
                    display: 'block',
                    borderLeft: thread.unread
                      ? '6px solid var(--mantine-color-blue-filled)'
                      : '1px solid var(--mantine-color-gray-3)',
                    backgroundColor: thread.unread
                      ? 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))'
                      : undefined,
                    transition: 'transform 0.15s ease, box-shadow 0.15s ease',
                  }}
                >
                  <Group justify="space-between" align="center" wrap="wrap" gap="md">
                    <Stack gap="xs" style={{ flex: 1, minWidth: 260 }}>
                      <Group gap="sm" wrap="wrap">
                        {thread.unread ? (
                          <Badge
                            color="red"
                            size="lg"
                            variant="filled"
                            styles={{ label: { fontSize: '0.95rem', fontWeight: 700 } }}
                          >
                            Nowa odpowiedź / Nieprzeczytana
                          </Badge>
                        ) : (
                          <Badge
                            color="gray"
                            size="lg"
                            variant="light"
                            leftSection={<IconCheck size={16} aria-hidden="true" />}
                            styles={{ label: { fontSize: '0.95rem', fontWeight: 600 } }}
                          >
                            Przeczytane
                          </Badge>
                        )}

                        {thread.relatedIdeaId && (
                          <Badge
                            color="blue"
                            size="lg"
                            variant="outline"
                            styles={{ label: { fontSize: '0.95rem' } }}
                          >
                            Dotyczy pomysłu
                          </Badge>
                        )}
                      </Group>

                      <Title order={3} size="h4" style={{ fontSize: '1.3rem', fontWeight: 700 }}>
                        {thread.subject}
                      </Title>

                      <Group gap="xs" align="center">
                        <IconClock size={18} color="var(--mantine-color-gray-6)" aria-hidden="true" />
                        <Text
                          component="time"
                          dateTime={thread.lastMessageAt}
                          title={formatPolishDateTime(thread.lastMessageAt)}
                          size="md"
                          c="dimmed"
                          style={{ fontSize: '1rem' }}
                        >
                          Ostatnia aktywność: {formatRelativePolishTime(thread.lastMessageAt)} ({formatPolishDateTime(thread.lastMessageAt)})
                        </Text>
                      </Group>
                    </Stack>

                    <Button
                      variant="light"
                      color="blue"
                      size="md"
                      rightSection={<IconArrowRight size={20} aria-hidden="true" />}
                      styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
                      tabIndex={-1}
                    >
                      Otwórz rozmowę
                    </Button>
                  </Group>
                </Card>
              </li>
            )
          })}
        </Stack>
      )}

      {/* New thread modal */}
      <NewThreadModal
        opened={newModalOpened}
        onClose={() => setNewModalOpened(false)}
        initialRelatedIdeaId={initialIdeaId}
        onThreadCreated={() => {
          setNewModalOpened(false)
          handleRefresh()
        }}
      />
    </Stack>
  )
}
