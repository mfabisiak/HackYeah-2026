import {
  Alert,
  Badge,
  Button,
  Group,
  Modal,
  Stack,
  Text,
  Textarea,
} from '@mantine/core'
import { IconCheck, IconSend } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import type { TestRequestJs } from 'hubmi-client'

export interface TestRequestModalProps {
  opened: boolean
  onClose: () => void
  innovationId: string
  innovationTitle: string
}

export function TestRequestModal({
  opened,
  onClose,
  innovationId,
  innovationTitle,
}: TestRequestModalProps) {
  const { authenticated, login } = useAuth()
  const [existingRequest, setExistingRequest] = useState<TestRequestJs | null>(null)
  const [note, setNote] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSuccess, setIsSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!opened || !authenticated) return
    let cancelled = false

    void hubApi.innovations.myTestRequest(innovationId).then((res) => {
      if (cancelled) return
      setIsSuccess(false)
      setError(null)
      if (res.value) {
        setExistingRequest(res.value)
        setNote(res.value.note ?? '')
      } else {
        setExistingRequest(null)
      }
    })

    return () => {
      cancelled = true
    }
  }, [opened, authenticated, innovationId])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)
    setError(null)

    try {
      const res = await hubApi.innovations.requestTest(innovationId, note.trim() || undefined)
      if (res.error) {
        setError(res.error.message || 'Nie udało się przesłać zgłoszenia. Spróbuj ponownie.')
      } else if (res.value) {
        setExistingRequest(res.value)
        setIsSuccess(true)
      }
    } catch {
      setError('Wystąpił błąd sieci. Spróbuj ponownie.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'NEW':
        return { label: 'Nowe – oczekuje na weryfikację przez ROPS', color: 'blue' }
      case 'ACCEPTED':
        return { label: 'Zaakceptowane – skontaktujemy się z Tobą!', color: 'teal' }
      case 'DECLINED':
        return { label: 'Odrzucone', color: 'red' }
      default:
        return { label: status, color: 'gray' }
    }
  }

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title="Zgłoszenie do testowania innowacji"
      size="lg"
      radius="md"
      styles={{
        title: { fontSize: '1.2rem', fontWeight: 700 },
      }}
    >
      <Stack gap="md">
        <Text size="md" style={{ lineHeight: 1.5 }}>
          Rozwiązanie: <strong>{innovationTitle}</strong>
        </Text>

        {!authenticated ? (
          <Alert color="blue" title="Wymagane logowanie" radius="md">
            <Stack gap="sm">
              <Text size="md">
                Aby zgłosić chęć udziału w testach innowacji społecznej, musisz być zalogowany.
              </Text>
              <div>
                <Button onClick={login} size="md">
                  Zaloguj się
                </Button>
              </div>
            </Stack>
          </Alert>
        ) : (
          <>
            {existingRequest && (
              <Alert
                color={getStatusLabel(existingRequest.status).color}
                title="Twoje zgłoszenie zostało zarejestrowane"
                radius="md"
              >
                <Stack gap="xs">
                  <Group gap="xs">
                    <Text size="sm" fw={600}>
                      Status:
                    </Text>
                    <Badge color={getStatusLabel(existingRequest.status).color} variant="light">
                      {getStatusLabel(existingRequest.status).label}
                    </Badge>
                  </Group>
                  {existingRequest.note && (
                    <Text size="sm">Twoja notatka: „{existingRequest.note}”</Text>
                  )}
                  <Text size="sm" c="dimmed">
                    Możesz zaktualizować notatkę poniżej, dopóki administrator nie podejmie decyzji.
                  </Text>
                </Stack>
              </Alert>
            )}

            {isSuccess && (
              <Alert
                color="teal"
                title="Dziękujemy!"
                icon={<IconCheck size={20} />}
                radius="md"
              >
                Twoje zgłoszenie do testów zostało pomyślnie zapisane.
              </Alert>
            )}

            {error && (
              <Alert color="red" title="Wystąpił błąd" radius="md">
                {error}
              </Alert>
            )}

            <form onSubmit={handleSubmit}>
              <Stack gap="md">
                <Textarea
                  id="tester-note"
                  label="Dlaczego chcesz przetestować to rozwiązanie? (opcjonalnie)"
                  description="Napisz krótko, kim jesteś (np. senior, opiekun, pracownik gminy) i w czym to rozwiązanie mogłoby pomóc."
                  placeholder="Np. Jestem sołtysem w małej wsi i chcielibyśmy sprawdzić ten model w naszym klubie seniora..."
                  value={note}
                  onChange={(e) => setNote(e.currentTarget.value)}
                  minRows={3}
                  maxRows={6}
                  disabled={isSubmitting}
                  size="md"
                  radius="md"
                  styles={{
                    label: { fontSize: '1rem', fontWeight: 600, marginBottom: 4 },
                    description: { fontSize: '0.9rem', marginBottom: 8 },
                  }}
                />

                <Group justify="flex-end" gap="sm">
                  <Button variant="default" onClick={onClose} disabled={isSubmitting}>
                    Zamknij
                  </Button>
                  <Button
                    type="submit"
                    loading={isSubmitting}
                    leftSection={<IconSend size={18} aria-hidden="true" />}
                    size="md"
                  >
                    {existingRequest ? 'Zaktualizuj zgłoszenie' : 'Wyślij zgłoszenie do testów'}
                  </Button>
                </Group>
              </Stack>
            </form>
          </>
        )}
      </Stack>
    </Modal>
  )
}
