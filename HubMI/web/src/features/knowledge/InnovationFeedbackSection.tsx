import {
  Alert,
  Box,
  Button,
  Card,
  Group,
  Radio,
  Stack,
  Text,
  Textarea,
  Title,
} from '@mantine/core'
import { IconCheck, IconStar, IconStarFilled } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import type { FeedbackJs } from 'hubmi-client'

export interface InnovationFeedbackSectionProps {
  innovationId: string
  averageRating?: number | null
  ratingsCount: number
}

function formatRatingCount(count: number): string {
  if (count === 1) return '1 ocena'
  const mod10 = count % 10
  const mod100 = count % 100
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
    return `${count} oceny`
  }
  return `${count} ocen`
}

const RATING_OPTIONS = [
  { value: '1', label: '1 – Słabo' },
  { value: '2', label: '2 – Przeciętnie' },
  { value: '3', label: '3 – Dobrze' },
  { value: '4', label: '4 – Bardzo dobrze' },
  { value: '5', label: '5 – Znakomicie' },
]

export function InnovationFeedbackSection({
  innovationId,
  averageRating,
  ratingsCount,
}: InnovationFeedbackSectionProps) {
  const { authenticated, login } = useAuth()
  const [userRating, setUserRating] = useState<string>('5')
  const [comment, setComment] = useState('')
  const [suggestion, setSuggestion] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSuccess, setIsSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [existingFeedback, setExistingFeedback] = useState<FeedbackJs | null>(null)

  useEffect(() => {
    if (!authenticated) return
    let cancelled = false

    void hubApi.innovations.myFeedback(innovationId).then((res) => {
      if (cancelled) return
      if (res.value) {
        setExistingFeedback(res.value)
        setUserRating(res.value.rating.toString())
        setComment(res.value.comment ?? '')
        setSuggestion(res.value.suggestion ?? '')
      }
    })

    return () => {
      cancelled = true
    }
  }, [authenticated, innovationId])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)
    setError(null)
    setIsSuccess(false)

    try {
      const res = await hubApi.innovations.sendFeedback(
        innovationId,
        parseInt(userRating, 10),
        comment.trim() || undefined,
        suggestion.trim() || undefined,
      )

      if (res.error) {
        if (res.error.status === 401) {
          login()
          return
        }
        setError(res.error.message || 'Nie udało się zapisać oceny. Spróbuj ponownie.')
      } else if (res.value) {
        setExistingFeedback(res.value)
        setIsSuccess(true)
      }
    } catch {
      setError('Wystąpił błąd sieci podczas zapisywania oceny.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card withBorder padding="xl" radius="md" component="section" aria-labelledby="feedback-heading">
      <Stack gap="lg">
        <Group justify="space-between" align="center" wrap="wrap" gap="md">
          <Stack gap={2}>
            <Title order={3} size="h3" id="feedback-heading">
              Opinie i oceny rozwiązania
            </Title>
            <Text
              size="md"
              style={{ fontSize: '1.05rem' }}
              c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
            >
              Dowiedz się, jak to rozwiązanie oceniają inni mieszkańcy i podziel się swoim zdaniem.
            </Text>
          </Stack>

          <Group gap="xs" align="center">
            <IconStarFilled size={24} color="#f59f00" aria-hidden="true" />
            <Text size="xl" fw={700} style={{ fontSize: '1.25rem' }}>
              {typeof averageRating === 'number' && averageRating > 0
                ? `${averageRating.toFixed(1).replace('.', ',')} na 5 (${formatRatingCount(ratingsCount)})`
                : 'Brak ocen'}
            </Text>
          </Group>
        </Group>

        {!authenticated ? (
          <Alert
            color="blue"
            title="Chcesz ocenić to rozwiązanie?"
            radius="md"
            styles={{
              title: { fontSize: '1.2rem', fontWeight: 700 },
            }}
          >
            <Stack gap="md">
              <Text size="md" style={{ fontSize: '1.1rem', lineHeight: 1.6 }}>
                Zaloguj się do systemu HubMI, aby móc wystawić ocenę i podzielić się swoją opinią o tej innowacji.
              </Text>
              <div>
                <Button size="lg" onClick={login} styles={{ root: { fontSize: '1.05rem' } }}>
                  Zaloguj się, aby ocenić
                </Button>
              </div>
            </Stack>
          </Alert>
        ) : (
          <Box component="form" onSubmit={handleSubmit}>
            <Stack gap="lg">
              {isSuccess && (
                <Alert
                  color="teal"
                  title="Dziękujemy za opinię!"
                  icon={<IconCheck size={26} aria-hidden="true" />}
                  radius="md"
                  styles={{
                    title: { fontSize: '1.25rem', fontWeight: 700 },
                    message: { fontSize: '1.1rem', lineHeight: 1.6 },
                  }}
                >
                  Twoja ocena została pomyślnie zapisana. Pomaga nam to promować najlepsze innowacje w Małopolsce.
                </Alert>
              )}

              {error && (
                <Alert
                  color="red"
                  title="Wystąpił problem"
                  radius="md"
                  styles={{
                    title: { fontSize: '1.2rem', fontWeight: 700 },
                    message: { fontSize: '1.1rem', lineHeight: 1.5 },
                  }}
                >
                  {error}
                </Alert>
              )}

              <Stack gap="xs">
                <Text component="label" id="rating-label" fw={700} style={{ fontSize: '1.15rem' }}>
                  Twoja ocena rozwiązania:
                </Text>
                <Radio.Group
                  value={userRating}
                  onChange={setUserRating}
                  aria-labelledby="rating-label"
                >
                  <Group gap="lg" wrap="wrap" mt="xs">
                    {RATING_OPTIONS.map((opt) => (
                      <Radio
                        key={opt.value}
                        value={opt.value}
                        label={opt.label}
                        size="md"
                        styles={{
                          label: { fontSize: '1.05rem', fontWeight: 600 },
                        }}
                      />
                    ))}
                  </Group>
                </Radio.Group>
              </Stack>

              <Textarea
                id="feedback-comment"
                label="Twój komentarz (opcjonalnie)"
                placeholder="Co najbardziej podoba Ci się w tym pomyśle? Co sprawdziło się w praktyce?"
                value={comment}
                onChange={(e) => setComment(e.currentTarget.value)}
                minRows={3}
                maxRows={6}
                disabled={isSubmitting}
                size="lg"
                radius="md"
                styles={{
                  label: { fontSize: '1.15rem', fontWeight: 700, marginBottom: 6 },
                  input: { fontSize: '1.1rem', lineHeight: 1.6 },
                }}
              />

              <Textarea
                id="feedback-suggestion"
                label="Co można usprawnić lub zmienić? (opcjonalnie)"
                placeholder="Twoje sugestie dotyczące poprawy lub rozszerzenia tego rozwiązania..."
                value={suggestion}
                onChange={(e) => setSuggestion(e.currentTarget.value)}
                minRows={3}
                maxRows={6}
                disabled={isSubmitting}
                size="lg"
                radius="md"
                styles={{
                  label: { fontSize: '1.15rem', fontWeight: 700, marginBottom: 6 },
                  input: { fontSize: '1.1rem', lineHeight: 1.6 },
                }}
              />

              <Group justify="flex-start" mt="sm">
                <Button
                  type="submit"
                  size="lg"
                  loading={isSubmitting}
                  leftSection={<IconStar size={22} aria-hidden="true" />}
                  styles={{
                    root: { fontSize: '1.05rem', fontWeight: 600 },
                  }}
                >
                  {existingFeedback ? 'Zaktualizuj ocenę' : 'Zapisz ocenę'}
                </Button>
              </Group>
            </Stack>
          </Box>
        )}
      </Stack>
    </Card>
  )
}
