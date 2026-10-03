import { useState } from 'react'
import { Button, Group, Paper, Stack, Text } from '@mantine/core'
import { IconCheck, IconThumbDown, IconThumbUp } from '@tabler/icons-react'
import { hubApi } from '../../api/hubApi'

export interface MatchFeedbackProps {
  needId: string
}

export function MatchFeedback({ needId }: MatchFeedbackProps) {
  const [currentHelpful, setCurrentHelpful] = useState<boolean | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [feedbackSent, setFeedbackSent] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const handleFeedback = async (helpful: boolean) => {
    setIsSubmitting(true)
    setErrorMessage(null)
    try {
      const res = await hubApi.matches.sendFeedback(needId, helpful)
      if (res.ok) {
        setCurrentHelpful(helpful)
        setFeedbackSent(true)
      } else {
        setErrorMessage('Nie udało się zapisać opinii. Spróbuj ponownie.')
      }
    } catch {
      setErrorMessage('Wystąpił błąd podczas wysyłania opinii.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Paper
      withBorder
      p="md"
      radius="md"
      bg="gray.0"
      role="region"
      aria-label="Opinia o wynikach dopasowania"
    >
      <Stack gap="xs" align="center">
        <Text size="sm" fw={600} ta="center">
          Czy przedstawione rozwiązania okazały się pomocne?
        </Text>

        <Group gap="sm" justify="center">
          <Button
            size="xs"
            variant={currentHelpful === true ? 'filled' : 'outline'}
            color="teal"
            leftSection={<IconThumbUp size={14} aria-hidden="true" />}
            onClick={() => handleFeedback(true)}
            loading={isSubmitting}
            aria-pressed={currentHelpful === true}
          >
            Tak, pomocne
          </Button>

          <Button
            size="xs"
            variant={currentHelpful === false ? 'filled' : 'outline'}
            color="gray"
            leftSection={<IconThumbDown size={14} aria-hidden="true" />}
            onClick={() => handleFeedback(false)}
            loading={isSubmitting}
            aria-pressed={currentHelpful === false}
          >
            Nie, szukam czegoś innego
          </Button>
        </Group>

        <div role="status" aria-live="polite">
          {feedbackSent && (
            <Text
              size="xs"
              c="teal.8"
              fw={500}
              style={{ display: 'flex', alignItems: 'center', gap: 4 }}
            >
              <IconCheck size={14} aria-hidden="true" />
              Dziękujemy za opinię! Pomaga nam to rozwijać bazę wiedzy i trafność podpowiedzi.
            </Text>
          )}
          {errorMessage && (
            <Text size="xs" c="red">
              {errorMessage}
            </Text>
          )}
        </div>
      </Stack>
    </Paper>
  )
}
