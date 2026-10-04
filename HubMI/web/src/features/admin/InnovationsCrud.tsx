import {
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Modal,
  MultiSelect,
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
import {
  INNOVATION_STAGE_NAMES,
  SOCIAL_AREA_NAMES,
  TARGET_GROUP_NAMES,
} from '../knowledge/constants'
import { accessibleBadgeStyles } from './constants'
import { UpsertInnovationJs, type InnovationSummaryJs } from 'hubmi-client'

export function InnovationsCrud() {
  const [items, setItems] = useState<InnovationSummaryJs[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // Create / Edit modal state
  const [modalOpened, setModalOpened] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [summary, setSummary] = useState('')
  const [description, setDescription] = useState('')
  const [stage, setStage] = useState('TESTED')
  const [areas, setAreas] = useState<string[]>([])
  const [targetGroups, setTargetGroups] = useState<string[]>([])
  const [region, setRegion] = useState('')
  const [mediaUrls, setMediaUrls] = useState('')
  const [innovativeness, setInnovativeness] = useState('')
  const [problemDiagnosis, setProblemDiagnosis] = useState('')
  const [futureVision, setFutureVision] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Delete modal state
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  useEffect(() => {
    let cancelled = false

    const fetchList = async () => {
      try {
        const res = await hubApi.innovations.list(undefined, undefined, undefined, 0, 50)
        if (cancelled) return
        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać bazy innowacji.')
        } else if (res.value) {
          setItems(res.value.items)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd podczas ładowania innowacji.')
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
    setSummary('')
    setDescription('')
    setStage('TESTED')
    setAreas([])
    setTargetGroups([])
    setRegion('')
    setMediaUrls('')
    setInnovativeness('')
    setProblemDiagnosis('')
    setFutureVision('')
    setFormError(null)
    setModalOpened(true)
  }

  const handleOpenEdit = async (item: InnovationSummaryJs) => {
    setEditingId(item.id)
    setTitle(item.title)
    setSummary(item.summary)
    setStage(item.stage)
    setAreas(item.areas)
    setTargetGroups(item.targetGroups)
    setFormError(null)
    setModalOpened(true)

    try {
      const res = await hubApi.innovations.get(item.id)
      if (res.value) {
        setDescription(res.value.description)
        setRegion(res.value.region || '')
        setMediaUrls(res.value.mediaUrls.join('\n'))
        setInnovativeness(res.value.innovativeness || '')
        setProblemDiagnosis(res.value.problemDiagnosis || '')
        setFutureVision(res.value.futureVision || '')
      }
    } catch {
      // Keep basic summary
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !summary.trim()) {
      setFormError('Tytuł oraz podsumowanie innowacji są polami wymaganymi.')
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    const urls = mediaUrls
      .split('\n')
      .map((u) => u.trim())
      .filter((u) => u.length > 0)

    const req = new UpsertInnovationJs(
      title.trim(),
      summary.trim(),
      description.trim() || summary.trim(),
      areas,
      targetGroups,
      stage,
      region.trim() || null,
      urls,
      innovativeness.trim() || null,
      problemDiagnosis.trim() || null,
      null,
      null,
      futureVision.trim() || null,
    )

    try {
      let res
      if (editingId) {
        res = await hubApi.innovations.update(editingId, req)
      } else {
        res = await hubApi.innovations.create(req)
      }

      if (res.error) {
        setFormError(res.error.message || 'Nie udało się zapisać innowacji.')
      } else {
        setModalOpened(false)
        handleRefresh()
      }
    } catch {
      setFormError('Wystąpił błąd sieci podczas zapisywania innowacji.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return
    setIsDeleting(true)

    try {
      const res = await hubApi.innovations.delete(deleteId)
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
            Innowacje społeczne ({items.length})
          </Title>
          <Text size="sm" c="dimmed">
            Zarządzaj bazą gotowych i testowanych rozwiązań prezentowanych mieszkańcom Małopolski.
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
            Dodaj nową innowację
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
        <LoadingState message="Wczytuję innowacje..." minHeight={200} />
      ) : (
        <Card withBorder padding={0} radius="md" style={{ overflowX: 'auto' }}>
          <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="lg">
            <Table.Thead>
              <Table.Tr>
                <Table.Th scope="col">Tytuł innowacji</Table.Th>
                <Table.Th scope="col">Etap</Table.Th>
                <Table.Th scope="col">Obszary</Table.Th>
                <Table.Th scope="col">Grupy docelowe</Table.Th>
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
                      {item.summary}
                    </Text>
                  </Table.Td>

                  <Table.Td style={{ minWidth: 160 }}>
                    <Badge variant="light" color="blue" size="md" styles={accessibleBadgeStyles}>
                      {INNOVATION_STAGE_NAMES[item.stage] || item.stage}
                    </Badge>
                  </Table.Td>

                  <Table.Td style={{ minWidth: 160 }}>
                    <Group gap={6} wrap="wrap">
                      {item.areas.map((a) => (
                        <Badge key={a} size="sm" variant="outline" color="gray" styles={accessibleBadgeStyles}>
                          {SOCIAL_AREA_NAMES[a] || a}
                        </Badge>
                      ))}
                    </Group>
                  </Table.Td>

                  <Table.Td style={{ minWidth: 160 }}>
                    <Group gap={6} wrap="wrap">
                      {item.targetGroups.map((g) => (
                        <Badge key={g} size="sm" variant="outline" color="teal" styles={accessibleBadgeStyles}>
                          {TARGET_GROUP_NAMES[g] || g}
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
                        onClick={() => void handleOpenEdit(item)}
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
        title={editingId ? 'Edytuj innowację społeczną' : 'Dodaj nową innowację społeczną'}
        size="xl"
        radius="md"
        transitionProps={{ duration: 0 }}
        styles={{ title: { fontSize: '1.3rem', fontWeight: 700 } }}
      >
        <form onSubmit={handleSubmit}>
          <Stack gap="md">
            {formError && <ErrorAlert message={formError} />}

            <TextInput
              label="Tytuł innowacji"
              placeholder="np. Cyfrowy Klub Seniora w sołectwach"
              required
              value={title}
              onChange={(e) => setTitle(e.currentTarget.value)}
              size="md"
            />

            <Textarea
              label="Krótkie podsumowanie (zajawka)"
              placeholder="1–2 zdania podsumowujące istotę rozwiązania..."
              required
              minRows={2}
              value={summary}
              onChange={(e) => setSummary(e.currentTarget.value)}
              size="md"
            />

            <Textarea
              label="Pełny opis rozwiązania"
              placeholder="Dokładny opis działania innowacji..."
              minRows={4}
              value={description}
              onChange={(e) => setDescription(e.currentTarget.value)}
              size="md"
            />

            <Group grow wrap="wrap">
              <Select
                label="Etap rozwoju innowacji"
                value={stage}
                onChange={(val) => val && setStage(val)}
                data={[
                  { value: 'IDEA', label: 'Nowy pomysł (w przygotowaniu)' },
                  { value: 'TESTED', label: 'W trakcie testowania' },
                  { value: 'IMPLEMENTED', label: 'Gotowe i działające rozwiązanie' },
                  { value: 'SCALED', label: 'Wdrożone i skalowane w gminach' },
                ]}
                size="md"
              />

              <TextInput
                label="Region / Gmina wdrożenia"
                placeholder="np. Kraków - Podgórze lub Powiat Wielicki"
                value={region}
                onChange={(e) => setRegion(e.currentTarget.value)}
                size="md"
              />
            </Group>

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

            <MultiSelect
              label="Grupy odbiorców"
              data={Object.entries(TARGET_GROUP_NAMES).map(([val, label]) => ({
                value: val,
                label,
              }))}
              value={targetGroups}
              onChange={setTargetGroups}
              searchable
              size="md"
            />

            <Textarea
              label="Materiały i linki multimedialne (jeden URL w wierszu)"
              placeholder="https://rops.krakow.pl/innowacje/poradnik.pdf&#10;https://youtube.com/watch?v=..."
              minRows={2}
              value={mediaUrls}
              onChange={(e) => setMediaUrls(e.currentTarget.value)}
              size="md"
            />

            <Divider label="Narracja formularza ROPS (opcjonalne)" labelPosition="center" />

            <Textarea
              label="Innowacyjność rozwiązania"
              placeholder="Dlaczego to rozwiązanie jest nowatorskie w skali regionu..."
              minRows={2}
              value={innovativeness}
              onChange={(e) => setInnovativeness(e.currentTarget.value)}
              size="md"
            />

            <Textarea
              label="Diagnoza problemu"
              placeholder="Jaki problem społeczny rozwiązuje innowacja..."
              minRows={2}
              value={problemDiagnosis}
              onChange={(e) => setProblemDiagnosis(e.currentTarget.value)}
              size="md"
            />

            <Textarea
              label="Wizja przyszłego wdrożenia i upowszechniania"
              placeholder="Jak innowacja może zostać zaadaptowana przez inne samorządy..."
              minRows={2}
              value={futureVision}
              onChange={(e) => setFutureVision(e.currentTarget.value)}
              size="md"
            />

            <Group justify="flex-end" gap="md" mt="md">
              <Button variant="default" onClick={() => setModalOpened(false)} disabled={isSubmitting}>
                Anuluj
              </Button>
              <Button type="submit" color="blue" loading={isSubmitting} leftSection={<IconCheck size={18} />}>
                {editingId ? 'Zapisz zmiany' : 'Utwórz innowację'}
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>

      {/* Delete / Archive Confirmation Modal */}
      <Modal
        opened={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Potwierdzenie archiwizacji innowacji"
        size="md"
        radius="md"
        transitionProps={{ duration: 0 }}
      >
        <Stack gap="md">
          <Text size="md">
            Czy na pewno chcesz zarchiwizować tę innowację społeczną? Pozycja zostanie
            wycofana z widoku publicznego, zachowując spójność historyczną danych.
          </Text>
          <Group justify="flex-end" gap="md" mt="md">
            <Button variant="default" onClick={() => setDeleteId(null)} disabled={isDeleting}>
              Anuluj
            </Button>
            <Button color="red" loading={isDeleting} onClick={handleDelete}>
              Archiwizuj innowację
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
