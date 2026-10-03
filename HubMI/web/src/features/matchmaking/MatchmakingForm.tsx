import { useState } from 'react'
import {
  Alert,
  Button,
  Group,
  Paper,
  Select,
  Stack,
  Text,
  Textarea,
} from '@mantine/core'
import { IconSearch, IconShieldExclamation, IconSparkles } from '@tabler/icons-react'
import { MALOPOLSKA_MUNICIPALITIES, SAMPLE_QUERIES } from './constants'

export interface MatchmakingFormProps {
  isLoading: boolean
  onSubmit: (description: string, municipality?: string) => void
}

const MIN_LENGTH = 5
const MAX_LENGTH = 2000

export function MatchmakingForm({ isLoading, onSubmit }: MatchmakingFormProps) {
  const [description, setDescription] = useState('')
  const [municipality, setMunicipality] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleSampleClick = (text: string) => {
    setDescription(text)
    setError(null)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = description.trim()
    if (!trimmed) {
      setError('Opis problemu jest wymagany.')
      return
    }
    if (trimmed.length < MIN_LENGTH) {
      setError(`Opis problemu jest zbyt krótki (minimum ${MIN_LENGTH} znaków).`)
      return
    }
    if (trimmed.length > MAX_LENGTH) {
      setError(`Opis problemu przekracza limit ${MAX_LENGTH} znaków.`)
      return
    }

    setError(null)
    onSubmit(trimmed, municipality || undefined)
  }

  const length = description.length
  const isOverLimit = length > MAX_LENGTH

  return (
    <Paper
      withBorder
      p={{ base: 'md', sm: 'xl' }}
      radius="md"
      shadow="xs"
      component="form"
      onSubmit={handleSubmit}
      noValidate
      aria-label="Formularz zgłoszenia problemu społecznego"
    >
      <Stack gap="md">
        <Alert
          color="yellow"
          variant="light"
          radius="md"
          icon={<IconShieldExclamation size={20} aria-hidden="true" />}
          title="Ochrona danych osobowych"
          id="privacy-notice"
        >
          <Text size="sm">
            Nie wpisuj imion i nazwisk, numerów PESEL, telefonów ani dokładnych adresów zamieszkania.
            Opisz wyłącznie naturę problemu społecznego.
          </Text>
        </Alert>

        <Stack gap="xs">
          <Text size="sm" fw={600} component="label" htmlFor="problem-description">
            Opisz problem swoimi słowami{' '}
            <Text component="span" c="red" aria-hidden="true">
              *
            </Text>
          </Text>

          <Text size="xs" c="dimmed" id="description-hint">
            Napisz, z jakim wyzwaniem społecznym mierzysz się w swojej społeczności, gminie lub organizacji.
          </Text>

          <Textarea
            id="problem-description"
            name="description"
            rows={5}
            placeholder="Np. Seniorzy w naszej gminie mają trudności z dojazdem do lekarza specjalisty po likwidacji lokalnych połączeń autobusowych..."
            value={description}
            onChange={(e) => {
              setDescription(e.target.value)
              if (error) setError(null)
            }}
            disabled={isLoading}
            aria-describedby="privacy-notice description-hint description-counter"
            aria-invalid={Boolean(error || isOverLimit)}
            error={error}
            size="md"
            radius="md"
          />

          <Group justify="space-between" align="center">
            <Text size="xs" c="dimmed">
              Wymagane minimum {MIN_LENGTH} znaków.
            </Text>
            <Text
              id="description-counter"
              size="xs"
              c={isOverLimit ? 'red' : 'dimmed'}
              fw={isOverLimit ? 600 : 400}
              aria-live="polite"
            >
              {length} / {MAX_LENGTH} znaków
            </Text>
          </Group>
        </Stack>

        <Stack gap="xs">
          <Text size="xs" fw={500} c="dimmed">
            Możesz też skorzystać z gotowego przykładu:
          </Text>
          <Group gap="xs" wrap="wrap">
            {SAMPLE_QUERIES.map((sample) => (
              <Button
                key={sample.label}
                type="button"
                variant="light"
                color="gray"
                size="xs"
                radius="xl"
                leftSection={<IconSparkles size={14} aria-hidden="true" />}
                onClick={() => handleSampleClick(sample.text)}
                disabled={isLoading}
              >
                {sample.label}
              </Button>
            ))}
          </Group>
        </Stack>

        <Select
          id="municipality-select"
          label="Gmina lub miasto w Małopolsce (opcjonalnie)"
          description="Pomaga dopasować rozwiązania przetestowane w podobnym typie gminy."
          placeholder="Wybierz lub pozostaw puste..."
          data={MALOPOLSKA_MUNICIPALITIES.map((name) => ({ value: name, label: name }))}
          value={municipality}
          onChange={setMunicipality}
          searchable
          clearable
          disabled={isLoading}
          size="sm"
          radius="md"
        />

        <Group justify="flex-end" mt="xs">
          <Button
            type="submit"
            size="md"
            loading={isLoading}
            leftSection={<IconSearch size={20} aria-hidden="true" />}
          >
            Znajdź rozwiązania
          </Button>
        </Group>
      </Stack>
    </Paper>
  )
}
