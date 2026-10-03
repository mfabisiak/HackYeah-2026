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
import { IconSearch, IconShieldCheck, IconSparkles } from '@tabler/icons-react'
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
      setError('Prosimy wpisać opis problemu przed wyszukiwaniem.')
      return
    }
    if (trimmed.length < MIN_LENGTH) {
      setError(`Opis problemu jest zbyt krótki (wymagane minimum ${MIN_LENGTH} znaków).`)
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
      radius="lg"
      shadow="sm"
      component="form"
      onSubmit={handleSubmit}
      noValidate
      aria-label="Formularz zgłoszenia problemu społecznego"
    >
      <Stack gap="lg">
        <Alert
          color="blue"
          variant="light"
          radius="md"
          icon={<IconShieldCheck size={24} aria-hidden="true" />}
          title="Dbamy o Twoją prywatność i bezpieczeństwo danych"
          id="privacy-notice"
          styles={{
            title: { fontSize: '1rem', fontWeight: 700 },
          }}
        >
          <Text size="md" style={{ lineHeight: 1.5 }}>
            Nie musisz podawać swoich danych osobowych: nazwiska, numeru PESEL, telefonu ani dokładnego adresu.
            Wystarczy ogólny opis problemu, np. brak transportu dla seniorów do ośrodka zdrowia.
          </Text>
        </Alert>

        <Stack gap="xs">
          <Text size="md" fw={700} component="label" htmlFor="problem-description">
            Opisz problem swoimi słowami{' '}
            <Text component="span" c="red" aria-hidden="true">
              *
            </Text>
          </Text>

          <Text size="sm" c="dimmed" id="description-hint">
            Napisz prostymi słowami, co sprawia trudność w codziennym życiu seniorom lub mieszkańcom Twojej okolicy.
          </Text>

          <Textarea
            id="problem-description"
            name="description"
            rows={5}
            placeholder="Np. Osoby starsze w naszej miejscowości mają problem z dotarciem do lekarza specjalisty lub apteki, bo nie ma bezpośredniego autobusu..."
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
            styles={{
              input: { fontSize: '1.05rem', lineHeight: 1.6 },
            }}
          />

          <Group justify="space-between" align="center">
            <Text size="sm" c="dimmed">
              Wymagane minimum {MIN_LENGTH} znaków.
            </Text>
            <Text
              id="description-counter"
              size="sm"
              c={isOverLimit ? 'red' : 'dimmed'}
              fw={isOverLimit ? 700 : 500}
              aria-live="polite"
            >
              {length} / {MAX_LENGTH} znaków
            </Text>
          </Group>
        </Stack>

        <Stack gap="xs">
          <Text size="sm" fw={600}>
            Możesz też wybrać gotowy przykład problemu (kliknij, aby wstawić):
          </Text>
          <Group gap="sm" wrap="wrap">
            {SAMPLE_QUERIES.map((sample) => (
              <Button
                key={sample.label}
                type="button"
                variant="light"
                color="blue"
                size="sm"
                radius="md"
                leftSection={<IconSparkles size={16} aria-hidden="true" />}
                onClick={() => handleSampleClick(sample.text)}
                disabled={isLoading}
                styles={{
                  root: { height: 38, fontSize: '0.95rem' },
                }}
              >
                {sample.label}
              </Button>
            ))}
          </Group>
        </Stack>

        <Select
          id="municipality-select"
          label="Twoja gmina lub miasto w Małopolsce (opcjonalnie)"
          description="Jeśli wskażesz gminę, system sprawdzi rozwiązania dopasowane do Twojego terenu."
          placeholder="Wybierz miejscowość lub pozostaw puste..."
          data={MALOPOLSKA_MUNICIPALITIES.map((name) => ({ value: name, label: name }))}
          value={municipality}
          onChange={setMunicipality}
          searchable
          clearable
          disabled={isLoading}
          size="md"
          radius="md"
          styles={{
            label: { fontSize: '0.95rem', fontWeight: 600 },
            description: { fontSize: '0.875rem' },
          }}
        />

        <Group justify="flex-end" mt="md">
          <Button
            type="submit"
            size="lg"
            loading={isLoading}
            leftSection={<IconSearch size={22} aria-hidden="true" />}
            styles={{
              root: { minHeight: 50, fontSize: '1.1rem', fontWeight: 700, paddingLeft: 24, paddingRight: 24 },
            }}
          >
            Wyszukaj sprawdzone rozwiązania
          </Button>
        </Group>
      </Stack>
    </Paper>
  )
}
