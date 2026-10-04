import {
  Alert,
  Badge,
  Button,
  Card,
  Drawer,
  Group,
  Pagination,
  Paper,
  Select,
  Stack,
  Table,
  Text,
  Textarea,
  Title,
} from '@mantine/core'
import {
  IconCheck,
  IconEdit,
  IconEye,
  IconInfoCircle,
  IconMessageCircle,
  IconNotes,
  IconRefresh,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toFriendlyErrorMessage } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { TARGET_GROUP_NAMES } from '../knowledge/constants'
import { IDEA_STAGE_LABELS, IDEA_STATUS_CONFIG } from './constants'
import type { IdeaJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function ExpertIdeasReview() {
  const [data, setData] = useState<PageJs<IdeaJs> | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // Drawer / Review modal state
  const [selectedIdea, setSelectedIdea] = useState<IdeaJs | null>(null)
  const [expertNote, setExpertNote] = useState('')
  const [noteSavedMessage, setNoteSavedMessage] = useState(false)

  useEffect(() => {
    let cancelled = false

    const fetchIdeas = async () => {
      try {
        const queryStatus = statusFilter === 'ALL' ? undefined : statusFilter
        const res = await hubApi.admin.ideas(queryStatus, currentPage - 1, PAGE_SIZE)
        if (cancelled) return
        if (res.error) {
          setError(toFriendlyErrorMessage(res.error, 'Nie udało się pobrać listy pomysłów do zaopiniowania.'))
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyErrorMessage(err, 'Wystąpił błąd podczas ładowania pomysłów.'))
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

  const handleOpenReview = (idea: IdeaJs) => {
    setSelectedIdea(idea)
    const saved = localStorage.getItem(`hubmi_expert_note_${idea.id}`) ?? ''
    setExpertNote(saved)
    setNoteSavedMessage(false)
  }

  const handleCloseReview = () => {
    setSelectedIdea(null)
    setNoteSavedMessage(false)
  }

  const handleSaveNote = () => {
    if (!selectedIdea) return
    localStorage.setItem(`hubmi_expert_note_${selectedIdea.id}`, expertNote)
    setNoteSavedMessage(true)
    setTimeout(() => setNoteSavedMessage(false), 3000)
  }

  const ideas = data?.items ?? []
  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  return (
    <Stack gap="lg">
      <Paper withBorder p="md" radius="md">
        <Group justify="space-between" wrap="wrap" gap="md">
          <Group gap="md">
            <Select
              label="Filtruj wg statusu formalnego"
              aria-label="Filtruj wg statusu formalnego"
              value={statusFilter}
              onChange={(val) => {
                setStatusFilter(val || 'ALL')
                setCurrentPage(1)
              }}
              data={[
                { value: 'ALL', label: 'Wszystkie pomysły' },
                { value: 'SUBMITTED', label: 'Zgłoszony (nowy)' },
                { value: 'IN_REVIEW', label: 'W trakcie weryfikacji' },
                { value: 'ACCEPTED', label: 'Zaakceptowany' },
                { value: 'REJECTED', label: 'Odrzucony' },
              ]}
              w={260}
              size="sm"
            />
          </Group>
          <Button
            variant="default"
            size="sm"
            leftSection={<IconRefresh size={16} aria-hidden="true" />}
            onClick={handleRefresh}
          >
            Odśwież listę
          </Button>
        </Group>
      </Paper>

      {isLoading ? (
        <LoadingState message="Wczytuję listę pomysłów do zaopiniowania..." minHeight={200} />
      ) : error ? (
        <ErrorAlert message={error} onRetry={handleRefresh} />
      ) : ideas.length === 0 ? (
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Text size="md" c="dimmed">
            Brak pomysłów spełniających wybrane kryteria w bazie zgłoszeń.
          </Text>
        </Card>
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="md">
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700 }}>
                  Tytuł pomysłu i istota
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Etap innowacji
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Grupy odbiorców
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Data zgłoszenia
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Status formalny
                </Table.Th>
                <Table.Th scope="col" style={{ fontSize: '1rem', fontWeight: 700, textAlign: 'center' }}>
                  Akcja eksperta
                </Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {ideas.map((idea) => {
                const cfg = IDEA_STATUS_CONFIG[idea.status] || {
                  label: idea.status,
                  color: 'gray',
                  description: '',
                }
                const formattedDate = new Date(idea.createdAt).toLocaleDateString('pl-PL', {
                  day: 'numeric',
                  month: 'short',
                  year: 'numeric',
                })
                const hasSavedNote = Boolean(localStorage.getItem(`hubmi_expert_note_${idea.id}`))

                return (
                  <Table.Tr key={idea.id}>
                    <Table.Td style={{ minWidth: 260 }}>
                      <Text fw={700} style={{ fontSize: '1.05rem' }}>
                        {idea.title}
                      </Text>
                      <Text size="sm" c="dimmed" lineClamp={2}>
                        {idea.essence}
                      </Text>
                      {hasSavedNote && (
                        <Badge size="xs" color="indigo" variant="light" mt={4} leftSection={<IconNotes size={12} />}>
                          Posiada notatkę ekspercką
                        </Badge>
                      )}
                    </Table.Td>

                    <Table.Td style={{ minWidth: 150, textAlign: 'center' }}>
                      <Group justify="center">
                        <Badge variant="outline" color="blue" size="sm">
                          {IDEA_STAGE_LABELS[idea.stage] || idea.stage}
                        </Badge>
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 160 }}>
                      <Group gap={4} wrap="wrap" justify="center">
                        {idea.targetGroups.map((tg) => (
                          <Badge key={tg} size="xs" variant="outline" color="gray">
                            {TARGET_GROUP_NAMES[tg] || tg}
                          </Badge>
                        ))}
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 120, textAlign: 'center' }}>
                      <Text size="sm">{formattedDate}</Text>
                    </Table.Td>

                    <Table.Td style={{ minWidth: 160, textAlign: 'center' }}>
                      <Group justify="center">
                        <Badge color={cfg.color} size="md" variant="filled">
                          {cfg.label}
                        </Badge>
                      </Group>
                    </Table.Td>

                    <Table.Td style={{ textAlign: 'center', minWidth: 180 }}>
                      <Group justify="center">
                        <Button
                          size="sm"
                          color="indigo"
                          variant="light"
                          onClick={() => handleOpenReview(idea)}
                          leftSection={<IconEye size={16} aria-hidden="true" />}
                          styles={{ root: { fontWeight: 600 } }}
                        >
                          Zaopiniuj
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
            aria-label="Nawigacja po stronach listy pomysłów"
            size="md"
          />
        </Group>
      )}

      {/* Review Drawer / Modal */}
      <Drawer
        opened={selectedIdea !== null}
        onClose={handleCloseReview}
        title={
          <Text fw={700} size="xl">
            Analiza merytoryczna pomysłu
          </Text>
        }
        position="right"
        size="lg"
        padding="xl"
      >
        {selectedIdea && (
          <Stack gap="lg">
            <Paper withBorder p="md" radius="md">
              <Stack gap="xs">
                <Text size="xs" tt="uppercase" fw={700} c="dimmed">
                  Tytuł zgłoszonej innowacji
                </Text>
                <Title order={2} size="h2">
                  {selectedIdea.title}
                </Title>
                <Group gap="xs" mt="xs">
                  <Badge variant="filled" color={IDEA_STATUS_CONFIG[selectedIdea.status]?.color ?? 'gray'}>
                    Status ROPS: {IDEA_STATUS_CONFIG[selectedIdea.status]?.label ?? selectedIdea.status}
                  </Badge>
                  <Badge variant="outline" color="blue">
                    Etap: {IDEA_STAGE_LABELS[selectedIdea.stage] ?? selectedIdea.stage}
                  </Badge>
                </Group>
              </Stack>
            </Paper>

            <Paper withBorder p="md" radius="md">
              <Stack gap="xs">
                <Text size="xs" tt="uppercase" fw={700} c="dimmed">
                  Grupy odbiorców
                </Text>
                <Group gap={6} wrap="wrap">
                  {selectedIdea.targetGroups.map((tg) => (
                    <Badge key={tg} size="sm" variant="light" color="indigo">
                      {TARGET_GROUP_NAMES[tg] || tg}
                    </Badge>
                  ))}
                </Group>
              </Stack>
            </Paper>

            <Paper withBorder p="md" radius="md">
              <Stack gap="xs">
                <Text size="xs" tt="uppercase" fw={700} c="dimmed">
                  Istota pomysłu i opis rozwiązania
                </Text>
                <Text size="md" style={{ lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                  {selectedIdea.essence}
                </Text>
              </Stack>
            </Paper>

            {selectedIdea.adminComment && (
              <Alert color="blue" title="Komentarz komisji ROPS" icon={<IconInfoCircle size={20} />}>
                <Text size="sm">{selectedIdea.adminComment}</Text>
              </Alert>
            )}

            <Alert
              color="indigo"
              icon={<IconInfoCircle size={24} aria-hidden="true" />}
              title="Rola eksperta merytorycznego"
              radius="md"
            >
              <Text size="sm" style={{ lineHeight: 1.5 }}>
                Jako ekspert merytoryczny oceniasz potencjał społeczny, metodykę i wykonalność rozwiązania.
                Formalne decyzje administracyjne (kwalifikacja do grantu) podejmuje komisja ROPS.
              </Text>
            </Alert>

            {/* Substantive Expert Notes */}
            <Paper withBorder p="md" radius="md">
              <Stack gap="sm">
                <Group justify="space-between" align="center">
                  <Text fw={700} size="sm" tt="uppercase" c="dimmed">
                    Moja notatka ekspercka / Rekomendacja merytoryczna
                  </Text>
                  <IconEdit size={16} color="gray" aria-hidden="true" />
                </Group>
                <Textarea
                  placeholder="Wpisz swoje uwagi merytoryczne, pytania do innowatora lub rekomendacje dla komisji ROPS..."
                  minRows={4}
                  autosize
                  maxRows={10}
                  value={expertNote}
                  onChange={(e) => setExpertNote(e.currentTarget.value)}
                  aria-label="Treść notatki eksperckiej"
                />
                <Group justify="space-between" align="center">
                  <Button
                    size="sm"
                    color="indigo"
                    onClick={handleSaveNote}
                    leftSection={<IconCheck size={16} aria-hidden="true" />}
                  >
                    Zapisz notatkę
                  </Button>
                  {noteSavedMessage && (
                    <Text size="sm" c="green" fw={600}>
                      Notatka została zapisana!
                    </Text>
                  )}
                </Group>
              </Stack>
            </Paper>

            {/* Direct Dialog with Author */}
            <Paper withBorder p="md" radius="md" style={{ backgroundColor: 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))' }}>
              <Stack gap="xs">
                <Text fw={700} size="md">
                  Potrzebujesz doprecyzować szczegóły z innowatorem?
                </Text>
                <Text size="sm" c="dimmed">
                  Skorzystaj z modułu aktywnej komunikacji, aby zadać autorowi pytania lub udzielić wskazówek mentoringowych.
                </Text>
                <Button
                  component={Link}
                  to="/wiadomosci"
                  size="sm"
                  variant="light"
                  color="blue"
                  leftSection={<IconMessageCircle size={16} aria-hidden="true" />}
                  mt="xs"
                >
                  Otwórz centrum wiadomości i dialogu
                </Button>
              </Stack>
            </Paper>
          </Stack>
        )}
      </Drawer>
    </Stack>
  )
}
