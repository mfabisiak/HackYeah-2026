import { useState } from 'react'
import { Alert, Box, Button, Center, Group, Paper, Stack, Text, Title } from '@mantine/core'
import { IconCheck, IconHeartHandshake, IconThumbDown, IconThumbUp } from '@tabler/icons-react'
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
        setErrorMessage('Nie udało się zapisać opinii. Prosimy spróbować ponownie.')
      }
    } catch {
      setErrorMessage('Wystąpił błąd połączenia podczas zapisywania opinii.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Paper
      withBorder
      p={{ base: 'lg', sm: 'xl' }}
      radius="lg"
      shadow="sm"
      role="region"
      aria-label="Ocena przydatności propozycji"
      style={{
        borderTopWidth: 4,
        borderTopColor: 'var(--mantine-color-blue-filled)',
      }}
      data-testid="match-feedback-card"
    >
      <Stack gap="md" align="center">
        <Center
          style={{
            width: 52,
            height: 52,
            borderRadius: '50%',
            backgroundColor: 'var(--mantine-color-blue-light)',
            color: 'var(--mantine-color-blue-filled)',
          }}
          aria-hidden="true"
        >
          <IconHeartHandshake size={28} />
        </Center>

        <Stack gap="xs" align="center">
          <Title order={3} size="h3" ta="center">
            Czy te propozycje były dla Ciebie pomocne?
          </Title>
          <Text size="md" c="dimmed" ta="center" maw={580}>
            Twoja opinia jest dla nas bardzo cenna. Dzięki niej możemy jeszcze lepiej dobierać
            sprawdzone rozwiązania dla mieszkańców Małopolski.
          </Text>
        </Stack>

        <Group gap="md" justify="center" wrap="wrap">
          <Button
            size="md"
            variant={currentHelpful === true ? 'filled' : 'outline'}
            color="teal"
            leftSection={<IconThumbUp size={20} aria-hidden="true" />}
            onClick={() => handleFeedback(true)}
            loading={isSubmitting}
            aria-pressed={currentHelpful === true}
            styles={{
              root: { minHeight: 46, fontSize: '1rem', fontWeight: 600 },
            }}
          >
            Tak, pomocne
          </Button>

          <Button
            size="md"
            variant={currentHelpful === false ? 'filled' : 'outline'}
            color="gray"
            leftSection={<IconThumbDown size={20} aria-hidden="true" />}
            onClick={() => handleFeedback(false)}
            loading={isSubmitting}
            aria-pressed={currentHelpful === false}
            styles={{
              root: { minHeight: 46, fontSize: '1rem', fontWeight: 600 },
            }}
          >
            Nie, szukam czegoś innego
          </Button>
        </Group>

        <Box role="status" aria-live="polite" w="100%" maw={520}>
          {feedbackSent && (
            <Alert
              variant="light"
              color="teal"
              radius="md"
              icon={<IconCheck size={20} aria-hidden="true" />}
              title="Dziękujemy za opinię!"
              styles={{
                title: { fontSize: '1.05rem', fontWeight: 700 },
                message: { fontSize: '0.95rem' },
              }}
            >
              Zapisaliśmy Twoją odpowiedź. Pomaga nam to polecać mieszkańcom coraz lepsze rozwiązania.
            </Alert>
          )}
          {errorMessage && (
            <Alert
              variant="light"
              color="red"
              radius="md"
              title="Wystąpił błąd"
              styles={{
                title: { fontSize: '1rem', fontWeight: 700 },
              }}
            >
              {errorMessage}
            </Alert>
          )}
        </Box>
      </Stack>
    </Paper>
  )
}
