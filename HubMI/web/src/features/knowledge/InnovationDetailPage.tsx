import {
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconArrowLeft,
  IconCheck,
  IconEye,
  IconFileText,
  IconHeartHandshake,
  IconHelpCircle,
  IconMapPin,
  IconPlayerPlay,
  IconRocket,
  IconSparkles,
  IconStar,
  IconStarFilled,
  IconTarget,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { toFriendlyErrorMessage } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import {
  INNOVATION_STAGE_NAMES,
  SOCIAL_AREA_NAMES,
  TARGET_GROUP_NAMES,
} from './constants'
import { InnovationFeedbackSection } from './InnovationFeedbackSection'
import { TestRequestModal } from './TestRequestModal'
import { AdaptationModal } from '../adaptations/AdaptationModal'
import type { InnovationJs, TestRequestJs } from 'hubmi-client'

function formatRatingCount(count: number): string {
  if (count === 1) return '1 ocena'
  const mod10 = count % 10
  const mod100 = count % 100
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
    return `${count} oceny`
  }
  return `${count} ocen`
}

export function InnovationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { authenticated } = useAuth()
  const [innovation, setInnovation] = useState<InnovationJs | null>(null)
  const [existingRequest, setExistingRequest] = useState<TestRequestJs | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [testModalOpened, setTestModalOpened] = useState(false)
  const [adaptationModalOpened, setAdaptationModalOpened] = useState(false)

  useEffect(() => {
    if (!id || !authenticated) return
    let cancelled = false

    void hubApi.innovations.myTestRequest(id).then((res) => {
      if (cancelled) return
      if (res.value) {
        setExistingRequest(res.value)
      } else {
        setExistingRequest(null)
      }
    }).catch(() => {
      if (!cancelled) {
        setExistingRequest(null)
      }
    })

    return () => {
      cancelled = true
    }
  }, [id, authenticated])

  useEffect(() => {
    if (!id) return
    let cancelled = false

    const fetchDetail = async () => {
      try {
        const res = await hubApi.innovations.get(id)
        if (cancelled) return

        if (res.error || !res.value) {
          setError(toFriendlyErrorMessage(res.error, 'Nie znaleziono wybranej innowacji społecznej.'))
          setInnovation(null)
        } else {
          setInnovation(res.value)
          setError(null)
          document.title = `${res.value.title} | HubMI`
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyErrorMessage(err, 'Wystąpił błąd podczas pobierania innowacji.'))
          setInnovation(null)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchDetail()

    return () => {
      cancelled = true
    }
  }, [id])

  if (isLoading) {
    return <LoadingState message="Wczytuję szczegóły innowacji społecznej..." minHeight={300} />
  }

  if (error || !innovation) {
    return (
      <Stack gap="md">
        <PageHeader
          title="Błąd ładowania innowacji"
          breadcrumbs={[
            { title: 'Strona główna', href: '/' },
            { title: 'Baza innowacji', href: '/innowacje' },
            { title: 'Błąd' },
          ]}
        />
        <ErrorAlert message={error || 'Innowacja nie istnieje lub została zarchiwizowana.'} />
        <div>
          <Button component={Link} to="/innowacje" variant="light" size="md">
            Wróć do listy innowacji
          </Button>
        </div>
      </Stack>
    )
  }

  return (
    <Stack gap="xl">
      <PageHeader
        title={innovation.title}
        subtitle="Szczegółowy opis rozwiązania społecznego zrealizowanego w Małopolsce."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Baza innowacji', href: '/innowacje' },
          { title: innovation.title },
        ]}
      />

      <Group justify="space-between" align="center" wrap="wrap" gap="md">
        <Button
          component={Link}
          to="/innowacje"
          variant="subtle"
          color="gray"
          size="lg"
          leftSection={<IconArrowLeft size={22} aria-hidden="true" />}
          styles={{ root: { fontSize: '1.05rem' } }}
        >
          Wróć do bazy innowacji
        </Button>

        <Group gap="sm" wrap="wrap">
          <Button
            component="a"
            href="#feedback-heading"
            variant="outline"
            color="blue"
            size="lg"
            leftSection={<IconStar size={22} aria-hidden="true" />}
            styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
          >
            Oceń rozwiązanie
          </Button>

          <Button
            onClick={() => setAdaptationModalOpened(true)}
            variant="light"
            color="grape"
            size="lg"
            leftSection={<IconSparkles size={22} aria-hidden="true" />}
            styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
          >
            Dostosuj do mojej instytucji
          </Button>

          <Button
            onClick={() => setTestModalOpened(true)}
            size="lg"
            color={existingRequest ? 'teal' : 'blue'}
            leftSection={
              existingRequest ? (
                <IconCheck size={22} aria-hidden="true" />
              ) : (
                <IconHeartHandshake size={22} aria-hidden="true" />
              )
            }
            styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
          >
            {existingRequest
              ? 'Zgłoszono do testów (edytuj zgłoszenie)'
              : 'Chcę przetestować to rozwiązanie'}
          </Button>
        </Group>
      </Group>

      {/* Main Overview Card */}
      <Card withBorder padding="xl" radius="md">
        <Stack gap="lg">
          <Group justify="space-between" align="center" wrap="wrap" gap="md">
            <Group gap="sm" wrap="wrap">
              <Badge
                variant="outline"
                color="gray"
                size="xl"
                radius="md"
                leftSection={<IconCheck size={18} aria-hidden="true" />}
                style={{
                  height: 38,
                  borderWidth: 2,
                  borderColor: 'var(--mantine-color-gray-6)',
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
                styles={{
                  label: { fontSize: '0.95rem', fontWeight: 600 },
                }}
              >
                {INNOVATION_STAGE_NAMES[innovation.stage] ?? innovation.stage}
              </Badge>

              {innovation.region && (
                <Badge
                  variant="light"
                  color="gray"
                  size="xl"
                  radius="md"
                  leftSection={<IconMapPin size={18} aria-hidden="true" />}
                  style={{ height: 38, display: 'inline-flex', alignItems: 'center' }}
                  styles={{
                    label: { fontSize: '0.95rem', fontWeight: 600 },
                  }}
                >
                  {innovation.region}
                </Badge>
              )}
            </Group>

            {innovation.averageRating && innovation.averageRating > 0 && (
              <Badge
                color="yellow"
                variant="light"
                size="xl"
                radius="md"
                style={{
                  height: 38,
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
                styles={{
                  label: { fontSize: '1.05rem', fontWeight: 600 },
                }}
              >
                <IconStarFilled size={18} aria-hidden="true" style={{ marginRight: 6 }} />
                {innovation.averageRating.toFixed(1).replace('.', ',')} na 5 ({formatRatingCount(innovation.ratingsCount)})
              </Badge>
            )}
          </Group>

          {/* Senior-friendly prominent summary block */}
          <Paper
            withBorder
            p="lg"
            radius="md"
            style={{
              backgroundColor: 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))',
              borderLeft: '5px solid var(--mantine-color-blue-filled)',
            }}
          >
            <Text size="lg" fw={500} style={{ lineHeight: 1.6, fontSize: '1.15rem' }}>
              {innovation.summary}
            </Text>
          </Paper>

          {innovation.areas && innovation.areas.length > 0 && (
            <Stack gap="xs">
              <Text size="md" fw={700}>
                Obszary tematyczne:
              </Text>
              <Group gap="sm" wrap="wrap">
                {innovation.areas.map((area) => (
                  <Badge
                    key={area}
                    variant="light"
                    color="blue"
                    size="xl"
                    radius="md"
                    style={{
                      height: 38,
                      border: '1.5px solid var(--mantine-color-blue-light-color)',
                      display: 'inline-flex',
                      alignItems: 'center',
                    }}
                    styles={{
                      label: { fontSize: '1.05rem', fontWeight: 600 },
                    }}
                  >
                    {SOCIAL_AREA_NAMES[area] ?? area}
                  </Badge>
                ))}
              </Group>
            </Stack>
          )}

          {innovation.targetGroups && innovation.targetGroups.length > 0 && (
            <Stack gap="xs">
              <Text size="lg" fw={700} style={{ fontSize: '1.15rem' }}>
                Grupy odbiorców i uczestników (Dla kogo):
              </Text>
              <Group gap="sm" wrap="wrap">
                {innovation.targetGroups.map((tg) => (
                  <Badge
                    key={tg}
                    variant="light"
                    color="teal"
                    size="xl"
                    radius="md"
                    style={{
                      height: 38,
                      border: '1.5px solid var(--mantine-color-teal-light-color)',
                      display: 'inline-flex',
                      alignItems: 'center',
                    }}
                    styles={{
                      label: { fontSize: '1.05rem', fontWeight: 600 },
                    }}
                  >
                    {TARGET_GROUP_NAMES[tg] ?? tg}
                  </Badge>
                ))}
              </Group>
            </Stack>
          )}

          <Divider my="sm" />

          <Stack gap="xs">
            <Title order={2} size="h3">
              Opis działania innowacji
            </Title>
            <Text size="md" style={{ lineHeight: 1.7, fontSize: '1.1rem' }}>
              {innovation.description}
            </Text>
          </Stack>
        </Stack>
      </Card>

      {/* Narrative ROPS Sections */}
      <Stack gap="lg">
        <Title order={2} size="h3">
          Kluczowe aspekty innowacji według formularza ROPS
        </Title>

        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg">
          {innovation.innovativeness && (
            <Card withBorder padding="lg" radius="md">
              <Stack gap="sm">
                <Group gap="xs">
                  <ThemeIcon color="grape" size={32} radius="md" variant="light">
                    <IconSparkles size={20} aria-hidden="true" />
                  </ThemeIcon>
                  <Title order={3} size="h4">
                    Innowacyjność rozwiązania
                  </Title>
                </Group>
                <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                  {innovation.innovativeness}
                </Text>
              </Stack>
            </Card>
          )}

          {innovation.problemDiagnosis && (
            <Card withBorder padding="lg" radius="md">
              <Stack gap="sm">
                <Group gap="xs">
                  <ThemeIcon color="orange" size={32} radius="md" variant="light">
                    <IconHelpCircle size={20} aria-hidden="true" />
                  </ThemeIcon>
                  <Title order={3} size="h4">
                    Diagnoza problemu
                  </Title>
                </Group>
                <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                  {innovation.problemDiagnosis}
                </Text>
              </Stack>
            </Card>
          )}

          {innovation.audienceDescription && (
            <Card withBorder padding="lg" radius="md">
              <Stack gap="sm">
                <Group gap="xs">
                  <ThemeIcon color="teal" size={32} radius="md" variant="light">
                    <IconTarget size={20} aria-hidden="true" />
                  </ThemeIcon>
                  <Title order={3} size="h4">
                    Opis odbiorców innowacji
                  </Title>
                </Group>
                <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                  {innovation.audienceDescription}
                </Text>
              </Stack>
            </Card>
          )}

          {innovation.expectedChange && (
            <Card withBorder padding="lg" radius="md">
              <Stack gap="sm">
                <Group gap="xs">
                  <ThemeIcon color="blue" size={32} radius="md" variant="light">
                    <IconRocket size={20} aria-hidden="true" />
                  </ThemeIcon>
                  <Title order={3} size="h4">
                    Zmiana, jaką wprowadza
                  </Title>
                </Group>
                <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                  {innovation.expectedChange}
                </Text>
              </Stack>
            </Card>
          )}
        </SimpleGrid>

        {innovation.futureVision && (
          <Card withBorder padding="lg" radius="md">
            <Stack gap="sm">
              <Group gap="xs">
                <ThemeIcon color="indigo" size={32} radius="md" variant="light">
                  <IconEye size={20} aria-hidden="true" />
                </ThemeIcon>
                <Title order={3} size="h4">
                  Wizja przyszłości i możliwości wdrożenia
                </Title>
              </Group>
              <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                {innovation.futureVision}
              </Text>
            </Stack>
          </Card>
        )}
      </Stack>

      {/* Media and Attached Files */}
      {innovation.mediaUrls && innovation.mediaUrls.length > 0 && (
        <Card withBorder padding="xl" radius="md" component="section" aria-labelledby="media-heading">
          <Stack gap="md">
            <Title order={3} size="h3" id="media-heading">
              Materiały do pobrania i multimedia
            </Title>
            <Text
              size="md"
              c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
            >
              Oficjalne materiały projektowe ROPS Kraków związane z tą innowacją.
            </Text>

            <Stack gap="sm">
              {innovation.mediaUrls.map((url, idx) => {
                const isVideo = url.includes('youtube') || url.includes('vimeo') || url.endsWith('.mp4')
                return (
                  <Paper key={idx} withBorder p="md" radius="md">
                    <Group justify="space-between" align="center" wrap="wrap" gap="sm">
                      <Group gap="sm">
                        <ThemeIcon
                          color={isVideo ? 'red' : 'blue'}
                          size={36}
                          radius="md"
                          variant="light"
                        >
                          {isVideo ? (
                            <IconPlayerPlay size={22} aria-hidden="true" />
                          ) : (
                            <IconFileText size={22} aria-hidden="true" />
                          )}
                        </ThemeIcon>
                        <div>
                          <Text fw={600} size="md">
                            {isVideo
                              ? 'Materiał wideo (z napisami i transkrypcją)'
                              : 'Podręcznik / broszura informacyjna (PDF)'}
                          </Text>
                          <Text
                            size="sm"
                            c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
                          >
                            {isVideo
                              ? 'Film instruktażowy z napisami w języku polskim (WCAG 1.2)'
                              : 'Dokument tekstowy dostępny cyfrowo'}
                          </Text>
                        </div>
                      </Group>

                      <Button
                        component="a"
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer"
                        variant="light"
                        size="sm"
                      >
                        {isVideo ? 'Obejrzyj materiał' : 'Pobierz dokument'}
                      </Button>
                    </Group>
                  </Paper>
                )
              })}
            </Stack>
          </Stack>
        </Card>
      )}

      {/* Rating & Feedback Section */}
      <InnovationFeedbackSection
        innovationId={innovation.id}
        averageRating={innovation.averageRating}
        ratingsCount={innovation.ratingsCount}
      />

      {/* Test Request Modal */}
      <TestRequestModal
        opened={testModalOpened}
        onClose={() => setTestModalOpened(false)}
        innovationId={innovation.id}
        innovationTitle={innovation.title}
        existingRequest={existingRequest}
        onRequestUpdated={setExistingRequest}
      />

      <AdaptationModal
        opened={adaptationModalOpened}
        onClose={() => setAdaptationModalOpened(false)}
        innovationId={innovation.id}
        innovationTitle={innovation.title}
      />
    </Stack>
  )
}
