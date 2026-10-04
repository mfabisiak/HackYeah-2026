import {
  Badge,
  Button,
  Card,
  Divider,
  Group,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import {
  IconClock,
  IconMessageHeart,
  IconRefresh,
  IconStarFilled,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { toFriendlyErrorMessage } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { AccessiblePagination } from '../../components/Pagination'
import { formatPolishDateTime } from '../messaging/constants'
import type { AdminFeedbackJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

const RATING_WORDS: Record<number, string> = {
  1: '1 – Słabo',
  2: '2 – Przeciętnie',
  3: '3 – Dobrze',
  4: '4 – Bardzo dobrze',
  5: '5 – Znakomicie',
}

export function FeedbackModerationQueue() {
  const [data, setData] = useState<PageJs<AdminFeedbackJs> | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  useEffect(() => {
    let cancelled = false

    const fetchFeedback = async () => {
      try {
        const res = await hubApi.admin.feedback(undefined, currentPage - 1, PAGE_SIZE)
        if (cancelled) return
        if (res.error) {
          setError(toFriendlyErrorMessage(res.error, 'Nie udało się pobrać opinii testerów.'))
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyErrorMessage(err, 'Wystąpił błąd podczas ładowania opinii testerów.'))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchFeedback()

    return () => {
      cancelled = true
    }
  }, [currentPage, refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setRefreshTrigger((prev) => prev + 1)
  }

  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="center" wrap="wrap" gap="md">
        <div>
          <Title order={2} size="h3" style={{ fontSize: '1.4rem', fontWeight: 700 }}>
            Oceny i opinie testerów innowacji
          </Title>
          <Text size="md" c="dimmed" style={{ fontSize: '1.05rem', marginTop: 2 }}>
            Recenzje rozwiązań wystawione przez mieszkańców i seniorów po zakończeniu testów.
          </Text>
        </div>

        <Button
          variant="default"
          size="md"
          onClick={handleRefresh}
          leftSection={<IconRefresh size={18} aria-hidden="true" />}
        >
          Odśwież opinie
        </Button>
      </Group>

      {error && <ErrorAlert message={error} />}

      {isLoading ? (
        <LoadingState message="Wczytuję opinie testerów..." minHeight={300} />
      ) : !data || data.items.length === 0 ? (
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Stack align="center" gap="sm" py="xl">
            <IconMessageHeart size={48} color="var(--mantine-color-gray-5)" aria-hidden="true" />
            <Title order={3} size="h4" style={{ fontSize: '1.25rem' }}>
              Brak opinii testerów
            </Title>
            <Text size="md" c="dimmed" style={{ fontSize: '1.05rem' }}>
              Żaden z testerów nie dodał jeszcze recenzji innowacji.
            </Text>
          </Stack>
        </Card>
      ) : (
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
          {data.items.map((fb) => (
            <Card key={fb.id} withBorder padding="lg" radius="md">
              <Stack gap="sm">
                <Group justify="space-between" align="flex-start" wrap="wrap" gap="xs">
                  <div>
                    <Text fw={700} size="md" style={{ fontSize: '1.15rem' }}>
                      {fb.innovationTitle}
                    </Text>
                    <Text size="xs" c="dimmed">
                      Tester: <code>{fb.userId}</code>
                    </Text>
                  </div>

                  <Badge
                    color={fb.rating >= 4 ? 'teal' : fb.rating === 3 ? 'yellow' : 'red'}
                    size="lg"
                    variant="light"
                    leftSection={<IconStarFilled size={14} aria-hidden="true" />}
                    styles={{ label: { fontSize: '0.9rem', fontWeight: 700 } }}
                  >
                    {RATING_WORDS[fb.rating] || `${fb.rating} / 5`}
                  </Badge>
                </Group>

                <Divider />

                <div>
                  <Text size="xs" c="dimmed" fw={600} tt="uppercase">
                    Komentarz testera
                  </Text>
                  <Text size="md" style={{ fontSize: '1.05rem', lineHeight: 1.5, marginTop: 4 }}>
                    {fb.comment || <Text span c="dimmed" fs="italic">Brak komentarza ogólnego</Text>}
                  </Text>
                </div>

                {fb.suggestion && (
                  <div>
                    <Text size="xs" c="dimmed" fw={600} tt="uppercase">
                      Sugestia usprawnienia
                    </Text>
                    <Text size="md" style={{ fontSize: '1.05rem', lineHeight: 1.5, marginTop: 4 }}>
                      {fb.suggestion}
                    </Text>
                  </div>
                )}

                <Group gap={6} align="center" mt="xs">
                  <IconClock size={16} color="var(--mantine-color-gray-6)" aria-hidden="true" />
                  <Text component="time" dateTime={fb.createdAt} size="xs" c="dimmed" style={{ fontSize: '0.9rem' }}>
                    Wystawiono: {formatPolishDateTime(fb.createdAt)}
                  </Text>
                </Group>
              </Stack>
            </Card>
          ))}
        </SimpleGrid>
      )}

      {totalPages > 1 && (
        <AccessiblePagination
          value={currentPage}
          total={totalPages}
          onChange={(page: number) => {
            setCurrentPage(page)
            setIsLoading(true)
          }}
        />
      )}
    </Stack>
  )
}
