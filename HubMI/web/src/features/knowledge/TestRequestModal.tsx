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
        if (res.error.status === 401) {
          login()
          return
        }
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
      size="xl"
      radius="md"
      trapFocus
      returnFocus
      closeOnEscape
      styles={{
        title: { fontSize: '1.4rem', fontWeight: 700 },
        header: { paddingBottom: 16 },
      }}
    >
      <Stack gap="lg">
        <Text style={{ fontSize: '1.2rem', lineHeight: 1.6 }}>
          Rozwiązanie:{' '}
          <strong style={{ color: 'var(--mantine-color-blue-filled)' }}>
            {innovationTitle}
          </strong>
        </Text>

        {!authenticated ? (
          <Alert
            color="blue"
            title="Wymagane logowanie"
            radius="md"
            styles={{
              title: { fontSize: '1.2rem', fontWeight: 700 },
            }}
          >
            <Stack gap="md">
              <Text style={{ fontSize: '1.1rem', lineHeight: 1.6 }}>
                Aby zgłosić chęć udziału w testach innowacji społecznej, musisz być zalogowany na platformie.
              </Text>
              <div>
                <Button onClick={login} size="lg" styles={{ root: { fontSize: '1.05rem' } }}>
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
                styles={{
                  title: { fontSize: '1.2rem', fontWeight: 700, marginBottom: 8 },
                }}
              >
                <Stack gap="sm">
                  <Group gap="sm" align="center" wrap="wrap">
                    <Text style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                      Status wniosku:
                    </Text>
                    <Badge
                      color={getStatusLabel(existingRequest.status).color}
                      variant="light"
                      size="xl"
                      radius="md"
                      style={{
                        height: 38,
                        display: 'inline-flex',
                        alignItems: 'center',
                        border: '1.5px solid currentColor',
                      }}
                      styles={{
                        label: { fontSize: '1.05rem', fontWeight: 600 },
                      }}
                    >
                      {getStatusLabel(existingRequest.status).label}
                    </Badge>
                  </Group>
                  {existingRequest.note && (
                    <Text style={{ fontSize: '1.1rem', lineHeight: 1.6 }}>
                      Twoja przesłana notatka: <strong>„{existingRequest.note}”</strong>
                    </Text>
                  )}
                  <Text
                    style={{ fontSize: '1.05rem', lineHeight: 1.5 }}
                    c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-gray-3))"
                  >
                    Możesz zaktualizować treść notatki poniżej, dopóki koordynator nie podejmie decyzji.
                  </Text>
                </Stack>
              </Alert>
            )}

            {isSuccess && (
              <Alert
                color="teal"
                title="Dziękujemy za zgłoszenie!"
                icon={<IconCheck size={26} />}
                radius="md"
                styles={{
                  title: { fontSize: '1.25rem', fontWeight: 700 },
                  message: { fontSize: '1.15rem', lineHeight: 1.6 },
                }}
              >
                Twoje zgłoszenie do testów zostało pomyślnie zapisane. Koordynator projektu skontaktuje się z Tobą.
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

            <form onSubmit={handleSubmit}>
              <Stack gap="lg">
                <Textarea
                  id="tester-note"
                  label="Dlaczego chcesz przetestować to rozwiązanie? (opcjonalnie)"
                  description="Napisz krótko, kim jesteś (np. senior, opiekun, pracownik gminy) i w czym to rozwiązanie mogłoby pomóc."
                  placeholder="Np. Jestem sołtysem w małej wsi i chcielibyśmy sprawdzić ten model w naszym klubie seniora..."
                  value={note}
                  onChange={(e) => {
                    setNote(e.currentTarget.value)
                    setIsSuccess(false)
                  }}
                  minRows={4}
                  maxRows={8}
                  disabled={isSubmitting}
                  size="lg"
                  radius="md"
                  styles={{
                    label: { fontSize: '1.15rem', fontWeight: 700, marginBottom: 6 },
                    description: { fontSize: '1.05rem', lineHeight: 1.5, marginBottom: 10 },
                    input: { fontSize: '1.1rem', lineHeight: 1.6 },
                  }}
                />

                <Group justify="flex-end" gap="md" mt="sm">
                  <Button
                    variant="default"
                    onClick={onClose}
                    disabled={isSubmitting}
                    size="lg"
                    styles={{
                      root: { fontSize: '1.05rem' },
                    }}
                  >
                    Zamknij
                  </Button>
                  <Button
                    type="submit"
                    loading={isSubmitting}
                    disabled={isSubmitting || isSuccess}
                    leftSection={<IconSend size={22} aria-hidden="true" />}
                    size="lg"
                    styles={{
                      root: { fontSize: '1.05rem', fontWeight: 600 },
                    }}
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
