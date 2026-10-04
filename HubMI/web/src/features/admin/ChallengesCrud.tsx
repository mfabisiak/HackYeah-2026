import {
  Badge,
  Button,
  Card,
  Group,
  Modal,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  Title,
} from '@mantine/core'
import {
  IconCheck,
  IconEdit,
  IconPlus,
  IconRefresh,
  IconTrash,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { SOCIAL_AREA_NAMES } from '../knowledge/constants'
import { UpsertChallengeJs, type ChallengeJs } from 'hubmi-client'

export function ChallengesCrud() {
  const [items, setItems] = useState<ChallengeJs[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // Create / Edit modal
  const [modalOpened, setModalOpened] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [area, setArea] = useState('AGING')
  const [municipalities, setMunicipalities] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Delete modal
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  useEffect(() => {
    let cancelled = false

    const fetchList = async () => {
      try {
        const res = await hubApi.challenges.list(undefined, 0, 50)
        if (cancelled) return
        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać wyzwań.')
        } else if (res.value) {
          setItems(res.value.items)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd podczas ładowania wyzwań.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchList()

    return () => {
      cancelled = true
    }
  }, [refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setRefreshTrigger((prev) => prev + 1)
  }

  const handleOpenCreate = () => {
    setEditingId(null)
    setTitle('')
    setDescription('')
    setArea('AGING')
    setMunicipalities('')
    setFormError(null)
    setModalOpened(true)
  }

  const handleOpenEdit = (item: ChallengeJs) => {
    setEditingId(item.id)
    setTitle(item.title)
    setDescription(item.description)
    setArea(item.area)
    setMunicipalities(item.municipalities.join(', '))
    setFormError(null)
    setModalOpened(true)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !description.trim()) {
      setFormError('Tytuł oraz opis wyzwania są polami wymaganymi.')
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    const munis = municipalities
      .split(',')
      .map((m) => m.trim())
      .filter((m) => m.length > 0)

    const req = new UpsertChallengeJs(title.trim(), description.trim(), area, munis)

    try {
      let res
      if (editingId) {
        res = await hubApi.challenges.update(editingId, req)
      } else {
        res = await hubApi.challenges.create(req)
      }

      if (res.error) {
        setFormError(res.error.message || 'Nie udało się zapisać wyzwania.')
      } else {
        setModalOpened(false)
        handleRefresh()
      }
    } catch {
      setFormError('Wystąpił błąd sieci.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return
    setIsDeleting(true)

    try {
      const res = await hubApi.challenges.delete(deleteId)
      if (!res.error) {
        setDeleteId(null)
        handleRefresh()
      }
    } catch {
      // Ignore
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="center" wrap="wrap" gap="md">
        <div>
          <Title order={3} size="h4" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
            Wyzwania społeczne Małopolski ({items.length})
          </Title>
          <Text size="sm" c="dimmed">
            Zgłoszone potrzeby i problemy gmin, dla których poszukiwane są innowacje.
          </Text>
        </div>

        <Group gap="sm">
          <Button
            onClick={handleOpenCreate}
            size="md"
            color="blue"
            leftSection={<IconPlus size={18} aria-hidden="true" />}
            styles={{ root: { fontWeight: 600 } }}
          >
            Dodaj nowe wyzwanie
          </Button>
          <Button
            variant="default"
            size="md"
            onClick={handleRefresh}
            leftSection={<IconRefresh size={18} aria-hidden="true" />}
          >
            Odśwież
          </Button>
        </Group>
      </Group>

      {error && <ErrorAlert message={error} />}

      {isLoading ? (
        <LoadingState message="Wczytuję wyzwania..." minHeight={200} />
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="lg">
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col">Tytuł wyzwania</Table.Th>
                <Table.Th scope="col">Obszar problemowy</Table.Th>
                <Table.Th scope="col">Gminy / Powiaty</Table.Th>
                <Table.Th scope="col" style={{ textAlign: 'right' }}>Akcje</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {items.map((item) => (
                <Table.Tr key={item.id}>
                  <Table.Td style={{ minWidth: 260 }}>
                    <Text fw={700} style={{ fontSize: '1.05rem' }}>
                      {item.title}
                    </Text>
                    <Text size="xs" c="dimmed" lineClamp={2}>
                      {item.description}
                    </Text>
                  </Table.Td>

                  <Table.Td style={{ minWidth: 160 }}>
                    <Badge variant="light" color="blue" size="md">
                      {SOCIAL_AREA_NAMES[item.area] || item.area}
                    </Badge>
                  </Table.Td>

                  <Table.Td style={{ minWidth: 160 }}>
                    <Group gap={4} wrap="wrap">
                      {item.municipalities.map((m) => (
                        <Badge key={m} size="xs" variant="outline" color="gray">
                          {m}
                        </Badge>
                      ))}
                    </Group>
                  </Table.Td>

                  <Table.Td style={{ textAlign: 'right', minWidth: 160 }}>
                    <Group gap="xs" justify="flex-end">
                      <Button
                        size="xs"
                        variant="light"
                        color="blue"
                        onClick={() => handleOpenEdit(item)}
                        leftSection={<IconEdit size={14} />}
                      >
                        Edytuj
                      </Button>
                      <Button
                        size="xs"
                        variant="subtle"
                        color="red"
                        onClick={() => setDeleteId(item.id)}
                        leftSection={<IconTrash size={14} />}
                      >
                        Archiwizuj
                      </Button>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
      )}

      {/* Create / Edit Modal */}
      <Modal
        opened={modalOpened}
        onClose={() => setModalOpened(false)}
        title={editingId ? 'Edytuj wyzwanie społeczne' : 'Dodaj nowe wyzwanie społeczne'}
        size="lg"
        radius="md"
        transitionProps={{ duration: 0 }}
      >
        <form onSubmit={handleSubmit}>
          <Stack gap="md">
            {formError && <ErrorAlert message={formError} />}

            <TextInput
              label="Tytuł wyzwania"
              placeholder="np. Samotność osób starszych w małych miejscowościach"
              required
              value={title}
              onChange={(e) => setTitle(e.currentTarget.value)}
              size="md"
            />

            <Textarea
              label="Opis wyzwania"
              placeholder="Szczegółowy opis zidentyfikowanego problemu..."
              required
              minRows={4}
              value={description}
              onChange={(e) => setDescription(e.currentTarget.value)}
              size="md"
            />

            <Select
              label="Główny obszar społeczny"
              value={area}
              onChange={(val) => val && setArea(val)}
              data={Object.entries(SOCIAL_AREA_NAMES).map(([val, label]) => ({
                value: val,
                label,
              }))}
              size="md"
            />

            <TextInput
              label="Gminy i powiaty (oddzielone przecinkami)"
              placeholder="np. Wieliczka, Niepołomice, Skawina"
              value={municipalities}
              onChange={(e) => setMunicipalities(e.currentTarget.value)}
              size="md"
            />

            <Group justify="flex-end" gap="md" mt="md">
              <Button variant="default" onClick={() => setModalOpened(false)} disabled={isSubmitting}>
                Anuluj
              </Button>
              <Button type="submit" color="blue" loading={isSubmitting} leftSection={<IconCheck size={18} />}>
                {editingId ? 'Zapisz zmiany' : 'Utwórz wyzwanie'}
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>

      {/* Delete / Archive Confirmation Modal */}
      <Modal
        opened={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Potwierdzenie archiwizacji wyzwania"
        size="md"
        radius="md"
        transitionProps={{ duration: 0 }}
      >
        <Stack gap="md">
          <Text size="md">
            Czy na pewno chcesz zarchiwizować to wyzwanie? Zostanie ono ukryte w katalogu publicznym.
          </Text>
          <Group justify="flex-end" gap="md" mt="md">
            <Button variant="default" onClick={() => setDeleteId(null)} disabled={isDeleting}>
              Anuluj
            </Button>
            <Button color="red" loading={isDeleting} onClick={handleDelete}>
              Archiwizuj wyzwanie
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
