import {
  Badge,
  Button,
  Card,
  Group,
  Modal,
  Paper,
  Select,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core'
import {
  IconCheck,
  IconClock,
  IconFileText,
  IconHeartHandshake,
  IconRefresh,
  IconX,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { AccessiblePagination } from '../../components/Pagination'
import { formatPolishDateTime } from '../messaging/constants'
import { accessibleBadgeStyles, TEST_REQUEST_STATUS_CONFIG } from './constants'
import type { AdminTestRequestJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function TestRequestsModerationQueue() {
  const [data, setData] = useState<PageJs<AdminTestRequestJs> | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)
  const [previewNoteItem, setPreviewNoteItem] = useState<AdminTestRequestJs | null>(null)

  // Decision confirmation modal state
  const [decisionItem, setDecisionItem] = useState<{
    item: AdminTestRequestJs
    targetStatus: 'ACCEPTED' | 'DECLINED'
  } | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [decisionError, setDecisionError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const fetchRequests = async () => {
      try {
        const queryStatus = statusFilter === 'ALL' ? undefined : statusFilter
        const res = await hubApi.admin.testRequests(undefined, queryStatus, currentPage - 1, PAGE_SIZE)
        if (cancelled) return
        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać listy zgłoszeń do testów.')
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd podczas ładowania zgłoszeń testerów.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchRequests()

    return () => {
      cancelled = true
    }
  }, [statusFilter, currentPage, refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setRefreshTrigger((prev) => prev + 1)
  }

  const handleConfirmDecision = async () => {
    if (!decisionItem) return
    setIsSubmitting(true)
    setDecisionError(null)

    try {
      const res = await hubApi.admin.decideTestRequest(
        decisionItem.item.id,
        decisionItem.targetStatus,
      )
      if (res.error) {
        setDecisionError(res.error.message || 'Nie udało się zapisać decyzji.')
      } else {
        setDecisionItem(null)
        handleRefresh()
      }
    } catch {
      setDecisionError('Błąd sieci podczas zapisywania decyzji.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="center" wrap="wrap" gap="md">
        <div>
          <Title order={2} size="h3" style={{ fontSize: '1.4rem', fontWeight: 700 }}>
            Zgłoszenia chęci testowania innowacji
          </Title>
          <Text size="md" c="dimmed" style={{ fontSize: '1.05rem', marginTop: 2 }}>
            Kwalifikacja mieszkańców, gmin i seniorów deklarujących gotowość do testowania rozwiązań.
          </Text>
        </div>

        <Group gap="sm" wrap="wrap">
          <Select
            label="Filtruj wg statusu"
            value={statusFilter}
            onChange={(val) => {
              if (val) {
                setStatusFilter(val)
                setCurrentPage(1)
                setIsLoading(true)
              }
            }}
            data={[
              { value: 'ALL', label: 'Wszystkie zgłoszenia' },
              { value: 'NEW', label: 'Nowe zgłoszenia' },
              { value: 'ACCEPTED', label: 'Zaakceptowane' },
              { value: 'DECLINED', label: 'Odrzucone' },
            ]}
            size="md"
            styles={{
              label: { fontSize: '0.95rem', fontWeight: 600, marginBottom: 4 },
              input: { fontSize: '1rem', minWidth: 200 },
            }}
          />

          <Button
            variant="default"
            size="md"
            onClick={handleRefresh}
            leftSection={<IconRefresh size={18} aria-hidden="true" />}
            style={{ alignSelf: 'flex-end', height: 42 }}
          >
            Odśwież
          </Button>
        </Group>
      </Group>

      {error && <ErrorAlert message={error} />}

      {isLoading ? (
        <LoadingState message="Wczytuję zgłoszenia testerów..." minHeight={300} />
      ) : !data || data.items.length === 0 ? (
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Stack align="center" gap="sm" py="xl">
            <IconHeartHandshake size={48} color="var(--mantine-color-teal-6)" aria-hidden="true" />
            <Title order={3} size="h4" style={{ fontSize: '1.25rem' }}>
              Brak zgłoszeń do testowania
            </Title>
            <Text size="md" c="dimmed" style={{ fontSize: '1.05rem' }}>
              {statusFilter === 'NEW'
                ? 'Wszystkie nowe zgłoszenia zostały już rozpatrzone.'
                : 'Brak zgłoszeń w wybranym filtrze.'}
            </Text>
          </Stack>
        </Card>
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="md" style={{ minWidth: 920 }}>
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col" style={{ width: '22%', fontSize: '1.05rem', fontWeight: 700 }}>
                  Rozwiązanie / Innowacja
                </Table.Th>
                <Table.Th scope="col" style={{ width: '14%', fontSize: '1.05rem', fontWeight: 700 }}>
                  Zgłaszający tester
                </Table.Th>
                <Table.Th scope="col" style={{ width: '30%', fontSize: '1.05rem', fontWeight: 700 }}>
                  Uzasadnienie (notatka)
                </Table.Th>
                <Table.Th scope="col" style={{ width: '13%', fontSize: '1.05rem', fontWeight: 700 }}>
                  Data zgłoszenia
                </Table.Th>
                <Table.Th scope="col" style={{ width: '8%', fontSize: '1.05rem', fontWeight: 700 }}>
                  Status
                </Table.Th>
                <Table.Th scope="col" style={{ width: '13%', fontSize: '1.05rem', fontWeight: 700, textAlign: 'right' }}>
                  Decyzja ROPS
                </Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {data.items.map((req) => {
                const cfg = TEST_REQUEST_STATUS_CONFIG[req.status] ?? {
                  label: req.status,
                  color: 'gray',
                }
                const isPending = req.status === 'NEW'

                return (
                  <Table.Tr key={req.id}>
                    <Table.Td style={{ minWidth: 180, wordBreak: 'break-word' }}>
                      <Text fw={700} size="md" style={{ fontSize: '1.05rem' }}>
                        {req.innovationTitle}
                      </Text>
                      <Text size="xs" c="dimmed">
                        ID: {req.innovationId}
                      </Text>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 120 }}>
                      <Text size="sm" fw={600} style={{ fontSize: '0.95rem' }}>
                        {req.userId}
                      </Text>
                    </Table.Td>

                    <Table.Td style={{ maxWidth: 300, minWidth: 220, wordBreak: 'break-word', overflowWrap: 'anywhere' }}>
                      {req.note ? (
                        <Stack gap={4}>
                          <Text
                            size="sm"
                            lineClamp={2}
                            style={{ fontSize: '0.95rem', lineHeight: 1.4 }}
                          >
                            {req.note}
                          </Text>
                          <Button
                            variant="light"
                            size="xs"
                            color="blue"
                            onClick={() => setPreviewNoteItem(req)}
                            leftSection={<IconFileText size={15} aria-hidden="true" />}
                            styles={{
                              root: {
                                height: 28,
                                paddingInline: 8,
                                fontSize: '0.85rem',
                                fontWeight: 600,
                                alignSelf: 'flex-start',
                              },
                            }}
                          >
                            Zobacz całe uzasadnienie
                          </Button>
                        </Stack>
                      ) : (
                        <Text size="sm" c="dimmed" fs="italic">
                          Brak dodatkowego uzasadnienia
                        </Text>
                      )}
                    </Table.Td>

                    <Table.Td style={{ minWidth: 130 }}>
                      <Group gap={6} align="center">
                        <IconClock size={16} color="var(--mantine-color-gray-6)" aria-hidden="true" />
                        <Text component="time" dateTime={req.createdAt} size="sm" style={{ fontSize: '0.95rem' }}>
                          {formatPolishDateTime(req.createdAt)}
                        </Text>
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 120 }}>
                      <Badge
                        color={cfg.color}
                        size="md"
                        variant="filled"
                        styles={accessibleBadgeStyles}
                      >
                        {cfg.label}
                      </Badge>
                    </Table.Td>

                    <Table.Td style={{ textAlign: 'right', whiteSpace: 'nowrap', minWidth: 190 }}>
                      {isPending ? (
                        <Group gap="xs" justify="flex-end" wrap="nowrap">
                          <Button
                            size="sm"
                            color="teal"
                            variant="light"
                            leftSection={<IconCheck size={16} />}
                            onClick={() =>
                              setDecisionItem({ item: req, targetStatus: 'ACCEPTED' })
                            }
                            styles={{ root: { fontSize: '0.95rem', fontWeight: 600 } }}
                          >
                            Zaakceptuj
                          </Button>
                          <Button
                            size="sm"
                            color="red"
                            variant="subtle"
                            leftSection={<IconX size={16} />}
                            onClick={() =>
                              setDecisionItem({ item: req, targetStatus: 'DECLINED' })
                            }
                            styles={{ root: { fontSize: '0.95rem', fontWeight: 600 } }}
                          >
                            Odrzuć
                          </Button>
                        </Group>
                      ) : (
                        <Text size="sm" c="dimmed" fs="italic">
                          Decyzja ostateczna
                        </Text>
                      )}
                    </Table.Td>
                  </Table.Tr>
                )
              })}
            </Table.Tbody>
          </Table>
        </Card>
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

      {/* Confirmation Modal */}
      <Modal
        opened={!!decisionItem}
        onClose={() => {
          if (!isSubmitting) setDecisionItem(null)
        }}
        title="Potwierdzenie decyzji o zgłoszeniu do testów"
        size="md"
        radius="md"
        transitionProps={{ duration: 0 }}
        styles={{ title: { fontSize: '1.25rem', fontWeight: 700 } }}
      >
        {decisionItem && (
          <Stack gap="md">
            <Text size="md" style={{ fontSize: '1.05rem', lineHeight: 1.6 }}>
              Czy na pewno chcesz{' '}
              <strong>
                {decisionItem.targetStatus === 'ACCEPTED' ? 'ZAAKCEPTOWAĆ' : 'ODRZUCIĆ'}
              </strong>{' '}
              zgłoszenie użytkownika <code>{decisionItem.item.userId}</code> do innowacji{' '}
              <strong>„{decisionItem.item.innovationTitle}”</strong>?
            </Text>

            <Text size="sm" c="dimmed">
              Uwaga: Decyzja jest wiążąca i użytkownik zobaczy zaktualizowany status w swoim profilu.
            </Text>

            {decisionItem.item.note && (
              <Paper withBorder p="sm" radius="md" bg="var(--mantine-color-gray-0)">
                <Text size="xs" c="dimmed" fw={700} tt="uppercase" mb={4}>
                  Uzasadnienie zgłoszenia przez testera:
                </Text>
                <Text size="sm" style={{ lineHeight: 1.5, wordBreak: 'break-word' }}>
                  {decisionItem.item.note}
                </Text>
              </Paper>
            )}

            {decisionError && <ErrorAlert message={decisionError} />}

            <Group justify="flex-end" gap="md" mt="md">
              <Button
                variant="default"
                onClick={() => setDecisionItem(null)}
                disabled={isSubmitting}
              >
                Anuluj
              </Button>
              <Button
                color={decisionItem.targetStatus === 'ACCEPTED' ? 'teal' : 'red'}
                loading={isSubmitting}
                onClick={handleConfirmDecision}
                styles={{ root: { fontWeight: 600 } }}
              >
                {decisionItem.targetStatus === 'ACCEPTED' ? 'Potwierdzam akceptację' : 'Potwierdzam odrzucenie'}
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>

      {/* Note Preview Modal - Safe against arbitrarily long text without breaking table layout */}
      <Modal
        opened={!!previewNoteItem}
        onClose={() => setPreviewNoteItem(null)}
        title="Uzasadnienie zgłoszenia do testów"
        size="lg"
        radius="md"
        transitionProps={{ duration: 0 }}
        styles={{ title: { fontSize: '1.25rem', fontWeight: 700 } }}
      >
        {previewNoteItem && (
          <Stack gap="md">
            <Paper withBorder p="md" radius="sm" bg="var(--mantine-color-gray-0)">
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
                <div>
                  <Text size="xs" c="dimmed" fw={600} tt="uppercase">Innowacja</Text>
                  <Text size="sm" fw={700}>{previewNoteItem.innovationTitle}</Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed" fw={600} tt="uppercase">Tester (zgłaszający)</Text>
                  <Text size="sm" fw={700}>{previewNoteItem.userId}</Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed" fw={600} tt="uppercase">Data zgłoszenia</Text>
                  <Text size="sm">{formatPolishDateTime(previewNoteItem.createdAt)}</Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed" fw={600} tt="uppercase">Aktualny status</Text>
                  <Badge
                    color={TEST_REQUEST_STATUS_CONFIG[previewNoteItem.status]?.color ?? 'gray'}
                    size="sm"
                    styles={accessibleBadgeStyles}
                  >
                    {TEST_REQUEST_STATUS_CONFIG[previewNoteItem.status]?.label ?? previewNoteItem.status}
                  </Badge>
                </div>
              </SimpleGrid>
            </Paper>

            <div>
              <Text size="sm" fw={700} mb={6}>Pełna treść uzasadnienia od testera:</Text>
              <Paper
                withBorder
                p="md"
                radius="md"
                style={{
                  maxHeight: 350,
                  overflowY: 'auto',
                  backgroundColor: 'white',
                  whiteSpace: 'pre-wrap',
                  lineHeight: 1.6,
                  fontSize: '1.02rem',
                }}
              >
                {previewNoteItem.note}
              </Paper>
            </div>

            <Group justify="space-between" mt="sm">
              <Button variant="default" size="md" onClick={() => setPreviewNoteItem(null)}>
                Zamknij podgląd
              </Button>

              {previewNoteItem.status === 'NEW' && (
                <Group gap="xs">
                  <Button
                    size="md"
                    color="teal"
                    leftSection={<IconCheck size={18} aria-hidden="true" />}
                    onClick={() => {
                      const item = previewNoteItem
                      setPreviewNoteItem(null)
                      setDecisionItem({ item, targetStatus: 'ACCEPTED' })
                    }}
                  >
                    Zaakceptuj wniosek
                  </Button>
                  <Button
                    size="md"
                    color="red"
                    variant="subtle"
                    leftSection={<IconX size={18} aria-hidden="true" />}
                    onClick={() => {
                      const item = previewNoteItem
                      setPreviewNoteItem(null)
                      setDecisionItem({ item, targetStatus: 'DECLINED' })
                    }}
                  >
                    Odrzuć wniosek
                  </Button>
                </Group>
              )}
            </Group>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}
