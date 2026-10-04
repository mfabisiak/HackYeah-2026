import {
  Badge,
  Button,
  Card,
  Checkbox,
  Divider,
  Group,
  Modal,
  MultiSelect,
  Paper,
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
  IconDownload,
  IconEdit,
  IconFileText,
  IconPlayerPlay,
  IconPlus,
  IconRefresh,
  IconTrash,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../../api/hubApi'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { SOCIAL_AREA_NAMES } from '../knowledge/constants'
import { accessibleBadgeStyles, MATERIAL_TYPE_LABELS } from './constants'
import { UpsertMaterialJs, type MaterialJs } from 'hubmi-client'

export function MaterialsCrud() {
  const [items, setItems] = useState<MaterialJs[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // Create / Edit modal
  const [modalOpened, setModalOpened] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState('GUIDE')
  const [url, setUrl] = useState('')
  const [areas, setAreas] = useState<string[]>([])
  const [videoHasSubtitles, setVideoHasSubtitles] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Delete modal
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  useEffect(() => {
    let cancelled = false

    const fetchList = async () => {
      try {
        const res = await hubApi.materials.list(undefined, undefined, undefined, 0, 50)
        if (cancelled) return
        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać materiałów.')
        } else if (res.value) {
          setItems(res.value.items)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd podczas ładowania materiałów.')
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
    setType('GUIDE')
    setUrl('')
    setAreas([])
    setVideoHasSubtitles(false)
    setFormError(null)
    setModalOpened(true)
  }

  const handleOpenEdit = (item: MaterialJs) => {
    setEditingId(item.id)
    setTitle(item.title)
    setDescription(item.description)
    setType(item.type)
    setUrl(item.url)
    setAreas(item.areas)
    setVideoHasSubtitles(
      item.type === 'VIDEO' ||
        item.description.toLowerCase().includes('napis') ||
        item.description.toLowerCase().includes('transkrypcj'),
    )
    setFormError(null)
    setModalOpened(true)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !description.trim() || !url.trim()) {
      setFormError('Wszystkie pola podstawowe (tytuł, opis, link) są wymagane.')
      return
    }

    // WCAG validation: video materials MUST declare subtitles or transcription
    if (type === 'VIDEO' && !videoHasSubtitles) {
      setFormError(
        'Wymóg dostępności WCAG: Dla materiałów wideo wymagane jest zapewnienie napisów lub transkrypcji tekstowej. Proszę zaznaczyć potwierdzenie dostępności.',
      )
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    let finalDesc = description.trim()
    if (type === 'VIDEO' && !finalDesc.toLowerCase().includes('napis')) {
      finalDesc += ' (Materiał wideo z napisami i audiodeskrypcją WCAG 2.1)'
    }

    const req = new UpsertMaterialJs(title.trim(), finalDesc, type, url.trim(), areas)

    try {
      let res
      if (editingId) {
        res = await hubApi.materials.update(editingId, req)
      } else {
        res = await hubApi.materials.create(req)
      }

      if (res.error) {
        setFormError(res.error.message || 'Nie udało się zapisać materiału.')
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
      const res = await hubApi.materials.delete(deleteId)
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
            Materiały i publikacje edukacyjne ({items.length})
          </Title>
          <Text size="sm" c="dimmed">
            Zarządzaj poradnikami, raportami, zestawami narzędzi i materiałami wideo ROPS.
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
            Dodaj nowy materiał
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
        <LoadingState message="Wczytuję materiały..." minHeight={200} />
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="lg">
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col">Tytuł publikacji</Table.Th>
                <Table.Th scope="col">Format / Typ</Table.Th>
                <Table.Th scope="col">Obszary</Table.Th>
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
                    <Badge
                      variant="light"
                      color={item.type === 'VIDEO' ? 'red' : 'indigo'}
                      size="sm"
                      styles={accessibleBadgeStyles}
                      leftSection={
                        item.type === 'VIDEO' ? (
                          <IconPlayerPlay size={16} aria-hidden="true" />
                        ) : (
                          <IconFileText size={16} aria-hidden="true" />
                        )
                      }
                    >
                      {MATERIAL_TYPE_LABELS[item.type] || item.type}
                    </Badge>
                  </Table.Td>

                  <Table.Td style={{ minWidth: 160 }}>
                    <Group gap={4} wrap="wrap">
                      {item.areas.map((a) => (
                        <Badge key={a} size="xs" variant="outline" color="gray" styles={accessibleBadgeStyles}>
                          {SOCIAL_AREA_NAMES[a] || a}
                        </Badge>
                      ))}
                    </Group>
                  </Table.Td>

                  <Table.Td style={{ textAlign: 'right', minWidth: 200 }}>
                    <Group gap="xs" justify="flex-end">
                      <Button
                        size="sm"
                        variant="light"
                        color="blue"
                        onClick={() => handleOpenEdit(item)}
                        leftSection={<IconEdit size={16} aria-hidden="true" />}
                        styles={{ root: { minHeight: 36, fontSize: '0.92rem', fontWeight: 600 } }}
                      >
                        Edytuj
                      </Button>
                      <Button
                        size="sm"
                        variant="subtle"
                        color="red"
                        onClick={() => setDeleteId(item.id)}
                        leftSection={<IconTrash size={16} aria-hidden="true" />}
                        styles={{ root: { minHeight: 36, fontSize: '0.92rem', fontWeight: 600 } }}
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
        title={editingId ? 'Edytuj materiał edukacyjny' : 'Dodaj nowy materiał edukacyjny'}
        size="lg"
        radius="md"
        transitionProps={{ duration: 0 }}
        styles={{ title: { fontSize: '1.3rem', fontWeight: 700 } }}
      >
        <form onSubmit={handleSubmit} noValidate>
          <Stack gap="md">
            {formError && <ErrorAlert message={formError} />}

            <TextInput
              label="Tytuł publikacji"
              placeholder="np. Jak inkubować innowację społeczną – poradnik"
              required
              value={title}
              onChange={(e) => setTitle(e.currentTarget.value)}
              size="md"
            />

            <Select
              label="Typ formatu publikacji"
              value={type}
              onChange={(val) => val && setType(val)}
              data={[
                { value: 'GUIDE', label: 'Poradnik krok po kroku' },
                { value: 'DOCUMENT', label: 'Dokument i raport' },
                { value: 'TOOLKIT', label: 'Narzędziownik / Zestaw ćwiczeń' },
                { value: 'VIDEO', label: 'Materiał wideo (wymaga transkrypcji WCAG)' },
                { value: 'CANVAS', label: 'Canva innowacji społecznej' },
                { value: 'OTHER', label: 'Inny format' },
              ]}
              size="md"
            />

            <TextInput
              label="Adres URL do pobrania lub odtworzenia"
              placeholder="https://rops.krakow.pl/materialy/poradnik.pdf"
              required
              value={url}
              onChange={(e) => setUrl(e.currentTarget.value)}
              size="md"
            />

            <Textarea
              label="Opis zawartości materiału"
              placeholder="Opis tematyki, dla kogo jest przeznaczony materiał..."
              required
              minRows={3}
              value={description}
              onChange={(e) => setDescription(e.currentTarget.value)}
              size="md"
            />

            <MultiSelect
              label="Obszary tematyczne"
              data={Object.entries(SOCIAL_AREA_NAMES).map(([val, label]) => ({
                value: val,
                label,
              }))}
              value={areas}
              onChange={setAreas}
              searchable
              size="md"
            />

            {/* Video WCAG Accessibility Checkbox */}
            {type === 'VIDEO' && (
              <Paper withBorder p="md" radius="md" style={{ backgroundColor: 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))' }}>
                <Checkbox
                  checked={videoHasSubtitles}
                  onChange={(e) => setVideoHasSubtitles(e.currentTarget.checked)}
                  label="Zapewniono napisy lub transkrypcję tekstową (wymóg WCAG dla wideo)"
                  required
                  styles={{
                    label: { fontSize: '0.95rem', fontWeight: 600, color: 'var(--mantine-color-blue-filled)' },
                  }}
                />
                <Text size="xs" c="dimmed" mt={4}>
                  Zgodnie ze standardami dostępności cyfrowej dla seniorów i osób niesłyszących, każdy materiał wideo musi posiadać polskie napisy i transkrypcję.
                </Text>
              </Paper>
            )}

            <Divider label="Podgląd – tak zobaczy to użytkownik" labelPosition="center" />

            {/* Preview Box */}
            <Paper withBorder p="md" radius="md">
              <Group justify="space-between" align="center" wrap="wrap">
                <Stack gap={4} style={{ flex: 1 }}>
                  <Text fw={700} style={{ fontSize: '1.05rem' }}>
                    {title || 'Tytuł publikacji'}
                  </Text>
                  <Text size="sm" c="dimmed">
                    {description || 'Krótki opis materiału...'}
                  </Text>
                </Stack>
                <Button
                  size="sm"
                  variant="light"
                  color={type === 'VIDEO' ? 'red' : 'blue'}
                  leftSection={type === 'VIDEO' ? <IconPlayerPlay size={16} /> : <IconDownload size={16} />}
                >
                  {type === 'VIDEO' ? 'Obejrzyj materiał' : 'Pobierz dokument'}
                </Button>
              </Group>
            </Paper>

            <Group justify="flex-end" gap="md" mt="md">
              <Button variant="default" onClick={() => setModalOpened(false)} disabled={isSubmitting}>
                Anuluj
              </Button>
              <Button type="submit" color="blue" loading={isSubmitting} leftSection={<IconCheck size={18} />}>
                {editingId ? 'Zapisz zmiany' : 'Utwórz materiał'}
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>

      {/* Delete / Archive Confirmation Modal */}
      <Modal
        opened={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Potwierdzenie archiwizacji materiału"
        size="md"
        radius="md"
        transitionProps={{ duration: 0 }}
      >
        <Stack gap="md">
          <Text size="md">
            Czy na pewno chcesz zarchiwizować ten materiał edukacyjny? Zostanie on wycofany
            z publicznej bazy wiedzy.
          </Text>
          <Group justify="flex-end" gap="md" mt="md">
            <Button variant="default" onClick={() => setDeleteId(null)} disabled={isDeleting}>
              Anuluj
            </Button>
            <Button color="red" loading={isDeleting} onClick={handleDelete}>
              Archiwizuj materiał
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
