import {
  Alert,
  Badge,
  Button,
  Card,
  Drawer,
  Group,
  Radio,
  Select,
  Stack,
  Table,
  Text,
  Textarea,
  Title,
} from '@mantine/core'
import {
  IconAlertCircle,
  IconCheck,
  IconClock,
  IconEdit,
  IconRefresh,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { toFriendlyErrorMessage } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { AccessiblePagination } from '../../components/Pagination'
import { formatPolishDateTime } from '../messaging/constants'
import { TARGET_GROUP_NAMES } from '../knowledge/constants'
import { accessibleBadgeStyles, IDEA_STATUS_CONFIG } from './constants'
import type { IdeaJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function IdeasModerationQueue() {
  const [data, setData] = useState<PageJs<IdeaJs> | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // Drawer state
  const [selectedIdea, setSelectedIdea] = useState<IdeaJs | null>(null)
  const [newStatus, setNewStatus] = useState<string>('')
  const [adminComment, setAdminComment] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [drawerError, setDrawerError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const fetchIdeas = async () => {
      try {
        const queryStatus = statusFilter === 'ALL' ? undefined : statusFilter
        const res = await hubApi.admin.ideas(queryStatus, currentPage - 1, PAGE_SIZE)
        if (cancelled) return
        if (res.error) {
          setError(toFriendlyErrorMessage(res.error, 'Nie udało się pobrać listy pomysłów.'))
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyErrorMessage(err, 'Wystąpił błąd podczas ładowania kolejki moderacji.'))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchIdeas()

    return () => {
      cancelled = true
    }
  }, [statusFilter, currentPage, refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setRefreshTrigger((prev) => prev + 1)
  }

  const handleOpenDrawer = (idea: IdeaJs) => {
    setSelectedIdea(idea)
    // In ROPS workflow, a newly submitted idea must first go into review
    if (idea.status === 'SUBMITTED') {
      setNewStatus('IN_REVIEW')
    } else {
      setNewStatus(idea.status)
    }
    setAdminComment(idea.adminComment || '')
    setDrawerError(null)
  }

  const handleCloseDrawer = () => {
    if (!isSubmitting) {
      setSelectedIdea(null)
      setDrawerError(null)
    }
  }

  const handleSubmitStatus = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedIdea || !newStatus) return

    if (newStatus === 'REJECTED' && !adminComment.trim()) {
      setDrawerError('Przy odrzuceniu pomysłu uzasadnienie dla autora jest wymagane.')
      return
    }

    setIsSubmitting(true)
    setDrawerError(null)

    try {
      const res = await hubApi.admin.updateIdeaStatus(
        selectedIdea.id,
        newStatus,
        adminComment.trim() || undefined,
      )

      if (res.error) {
        if (res.error.message && res.error.message.includes('Niedozwolona zmiana statusu')) {
          setDrawerError(res.error.message)
        } else if (res.error.status === 409) {
          setDrawerError('Ktoś już zmienił ten status — odśwież listę pomysłów.')
        } else {
          setDrawerError(toFriendlyErrorMessage(res.error, 'Nie udało się zaktualizować statusu pomysłu.'))
        }
      } else if (res.value) {
        handleCloseDrawer()
        handleRefresh()
      }
    } catch (err) {
      setDrawerError(toFriendlyErrorMessage(err, 'Wystąpił błąd sieci podczas zapisywania decyzji.'))
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
            Kolejka moderacji pomysłów
          </Title>
          <Text size="md" c="dimmed" style={{ fontSize: '1.05rem', marginTop: 2 }}>
            Weryfikacja formalna zgłoszeń innowacji społecznych, kwalifikacja i feedback dla autorów.
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
              { value: 'ALL', label: 'Wszystkie statusy' },
              { value: 'SUBMITTED', label: 'Zgłoszone (Nowe)' },
              { value: 'IN_REVIEW', label: 'W trakcie weryfikacji' },
              { value: 'ACCEPTED', label: 'Zaakceptowane' },
              { value: 'REJECTED', label: 'Odrzucone' },
            ]}
            size="md"
            styles={{
              label: { fontSize: '0.95rem', fontWeight: 600, marginBottom: 4 },
              input: { fontSize: '1rem', minWidth: 220 },
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
        <LoadingState message="Wczytuję pomysły do moderacji..." minHeight={300} />
      ) : !data || data.items.length === 0 ? (
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Stack align="center" gap="sm" py="xl">
            <IconCheck size={48} color="var(--mantine-color-teal-6)" aria-hidden="true" />
            <Title order={3} size="h4" style={{ fontSize: '1.25rem' }}>
              Brak pomysłów w wybranej kategorii
            </Title>
            <Text size="md" c="dimmed" style={{ fontSize: '1.05rem' }}>
              {statusFilter === 'SUBMITTED'
                ? 'Wszystkie nowe pomysły zostały już rozpatrzone.'
                : 'Brak zgłoszeń odpowiadających wybranym kryteriom filtra.'}
            </Text>
          </Stack>
        </Card>
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="lg">
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col" style={{ fontSize: '1.05rem', fontWeight: 700 }}>
                  Tytuł pomysłu i istota
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1.05rem', fontWeight: 700 }}>
                  Grupy odbiorców
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1.05rem', fontWeight: 700 }}>
                  Data zgłoszenia
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1.05rem', fontWeight: 700, textAlign: 'center' }}>
                  Status
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1.05rem', fontWeight: 700, textAlign: 'center' }}>
                  Akcja
                </Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {data.items.map((idea) => {
                const cfg = IDEA_STATUS_CONFIG[idea.status] ?? {
                  label: idea.status,
                  color: 'gray',
                }

                return (
                  <Table.Tr key={idea.id}>
                    <Table.Td style={{ minWidth: 260 }}>
                      <Text fw={700} size="md" style={{ fontSize: '1.1rem' }}>
                        {idea.title}
                      </Text>
                      <Text size="sm" c="dimmed" lineClamp={2} style={{ fontSize: '0.95rem', marginTop: 4 }}>
                        {idea.essence}
                      </Text>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 180 }}>
                      <Group gap={6} wrap="wrap">
                        {idea.targetGroups.map((g) => (
                          <Badge key={g} size="sm" variant="light" color="blue" styles={accessibleBadgeStyles}>
                            {TARGET_GROUP_NAMES[g] ?? g}
                          </Badge>
                        ))}
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 150 }}>
                      <Group gap={6} align="center">
                        <IconClock size={16} color="var(--mantine-color-gray-6)" aria-hidden="true" />
                        <Text
                          component="time"
                          dateTime={idea.createdAt}
                          size="sm"
                          style={{ fontSize: '0.95rem' }}
                        >
                          {formatPolishDateTime(idea.createdAt)}
                        </Text>
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 160, textAlign: 'center' }}>
                      <Group justify="center">
                        <Badge
                          color={cfg.color}
                          size="md"
                          variant="filled"
                          styles={accessibleBadgeStyles}
                        >
                          {cfg.label}
                        </Badge>
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ textAlign: 'center', minWidth: 150 }}>
                      <Group justify="center">
                        <Button
                          size="sm"
                          color="blue"
                          variant="light"
                          onClick={() => handleOpenDrawer(idea)}
                          leftSection={<IconEdit size={16} aria-hidden="true" />}
                          styles={{ root: { fontSize: '0.95rem', fontWeight: 600 } }}
                        >
                          Zmień status
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
        <AccessiblePagination
          value={currentPage}
          total={totalPages}
          onChange={(page: number) => {
            setCurrentPage(page)
            setIsLoading(true)
          }}
        />
      )}

      {/* Decision Drawer */}
      <Drawer
        opened={!!selectedIdea}
        onClose={handleCloseDrawer}
        position="right"
        size="lg"
        transitionProps={{ duration: 0 }}
        title="Weryfikacja i zmiana statusu pomysłu"
        styles={{
          title: { fontSize: '1.35rem', fontWeight: 700 },
          header: { padding: '20px', borderBottom: '1px solid var(--mantine-color-gray-3)' },
          body: { padding: '24px' },
        }}
      >
        {selectedIdea && (
          <form onSubmit={handleSubmitStatus} noValidate>
            <Stack gap="lg">
              <Card withBorder padding="md" radius="md">
                <Stack gap="xs">
                  <Text size="xs" c="dimmed" fw={600} tt="uppercase">
                    Tytuł pomysłu
                  </Text>
                  <Text fw={700} style={{ fontSize: '1.25rem' }}>
                    {selectedIdea.title}
                  </Text>
                  <Text size="sm" c="dimmed" style={{ fontSize: '1.05rem', lineHeight: 1.5 }}>
                    {selectedIdea.essence}
                  </Text>
                  {selectedIdea.targetGroups && selectedIdea.targetGroups.length > 0 && (
                    <div>
                      <Text size="xs" c="dimmed" fw={600} tt="uppercase" mt="xs" mb={4}>
                        Grupy odbiorców
                      </Text>
                      <Group gap={6} wrap="wrap">
                        {selectedIdea.targetGroups.map((g) => (
                          <Badge key={g} size="sm" variant="light" color="blue">
                            {TARGET_GROUP_NAMES[g] ?? g}
                          </Badge>
                        ))}
                      </Group>
                    </div>
                  )}
                </Stack>
              </Card>

              {drawerError && (
                <Alert
                  icon={<IconAlertCircle size={20} />}
                  color="red"
                  title="Błąd zmiany statusu"
                  radius="md"
                  role="alert"
                >
                  {drawerError}
                </Alert>
              )}

              {selectedIdea.status === 'SUBMITTED' && (
                <Alert color="blue" radius="md" title="Procedura weryfikacji nowego pomysłu ROPS">
                  Ten pomysł jest nowo zgłoszony. Zgodnie z procedurą ROPS pierwszym krokiem jest skierowanie go do weryfikacji (status: <strong>W trakcie weryfikacji</strong>). Po przeprowadzeniu weryfikacji odblokuje się możliwość akceptacji lub odrzucenia.
                </Alert>
              )}

              {(selectedIdea.status === 'ACCEPTED' || selectedIdea.status === 'REJECTED') && (
                <Alert color="gray" radius="md" title="Decyzja ostateczna">
                  Ten pomysł został już ostatecznie rozpatrzony (status: <strong>{IDEA_STATUS_CONFIG[selectedIdea.status]?.label ?? selectedIdea.status}</strong>). Jego status nie podlega dalszym modyfikacjom.
                </Alert>
              )}

              <div>
                <Text fw={600} style={{ fontSize: '1.1rem', marginBottom: 8 }}>
                  Wybierz nowy status pomysłu:
                </Text>
                <Radio.Group
                  value={newStatus}
                  onChange={setNewStatus}
                  name="ideaStatus"
                >
                  <Stack gap="sm">
                    <Radio
                      value="IN_REVIEW"
                      label="W trakcie weryfikacji (analiza formalna / ocena merytoryczna)"
                      description={selectedIdea.status === 'IN_REVIEW' ? 'Aktualny status pomysłu' : undefined}
                      size="md"
                      disabled={selectedIdea.status === 'IN_REVIEW' || selectedIdea.status === 'ACCEPTED' || selectedIdea.status === 'REJECTED'}
                      styles={{ label: { fontSize: '1rem', fontWeight: 500 } }}
                    />
                    <Radio
                      value="ACCEPTED"
                      label="Zaakceptowany (kwalifikacja do inkubacji / bazy)"
                      description={
                        selectedIdea.status === 'SUBMITTED'
                          ? 'Wymaga uprzedniego skierowania pomysłu do weryfikacji'
                          : undefined
                      }
                      disabled={selectedIdea.status === 'SUBMITTED' || selectedIdea.status === 'ACCEPTED' || selectedIdea.status === 'REJECTED'}
                      size="md"
                      styles={{ label: { fontSize: '1rem', fontWeight: 500 } }}
                    />
                    <Radio
                      value="REJECTED"
                      label="Odrzucony (wymaga podania uzasadnienia)"
                      description={
                        selectedIdea.status === 'SUBMITTED'
                          ? 'Wymaga uprzedniego skierowania pomysłu do weryfikacji'
                          : undefined
                      }
                      disabled={selectedIdea.status === 'SUBMITTED' || selectedIdea.status === 'ACCEPTED' || selectedIdea.status === 'REJECTED'}
                      size="md"
                      styles={{ label: { fontSize: '1rem', fontWeight: 500 } }}
                    />
                  </Stack>
                </Radio.Group>
              </div>

              <Textarea
                label="Komentarz administratora dla autora pomysłu"
                description={
                  newStatus === 'REJECTED'
                    ? 'Wymagane: wyjaśnij autorowi przyczynę odrzucenia oraz wskaż co może poprawić.'
                    : 'Opcjonalne: wskazówki lub zalecenia dla autora.'
                }
                placeholder="Wpisz treść informacji zwrotnej..."
                minRows={4}
                required={newStatus === 'REJECTED'}
                value={adminComment}
                onChange={(e) => {
                  setAdminComment(e.currentTarget.value)
                  if (drawerError) setDrawerError(null)
                }}
                size="md"
                styles={{
                  label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 4 },
                  input: { fontSize: '1.05rem', lineHeight: 1.5 },
                }}
              />

              <Group justify="flex-end" gap="md" mt="md">
                <Button variant="default" onClick={handleCloseDrawer} size="lg" disabled={isSubmitting}>
                  Anuluj
                </Button>
                <Button
                  type="submit"
                  size="lg"
                  color="blue"
                  loading={isSubmitting}
                  styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
                >
                  Zapisz decyzję
                </Button>
              </Group>
            </Stack>
          </form>
        )}
      </Drawer>
    </Stack>
  )
}
