import {
  Alert,
  Button,
  Group,
  Modal,
  Stack,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core'
import { IconAlertCircle, IconSend } from '@tabler/icons-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'

export interface NewThreadModalProps {
  opened: boolean
  onClose: () => void
  onThreadCreated?: (threadId: string) => void
  initialRelatedIdeaId?: string
  initialSubject?: string
}

export function NewThreadModal({
  opened,
  onClose,
  onThreadCreated,
  initialRelatedIdeaId,
  initialSubject = '',
}: NewThreadModalProps) {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [subject, setSubject] = useState(initialSubject)
  const [message, setMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [validationErrors, setValidationErrors] = useState<{
    subject?: string
    message?: string
  }>({})

  const validate = () => {
    const errors: { subject?: string; message?: string } = {}
    if (!subject.trim()) {
      errors.subject = 'Proszę podać temat rozmowy.'
    } else if (subject.trim().length < 3) {
      errors.subject = 'Temat musi mieć co najmniej 3 znaki.'
    }

    if (!message.trim()) {
      errors.message = 'Proszę wpisać treść wiadomości.'
    } else if (message.trim().length < 5) {
      errors.message = 'Wiadomość musi mieć co najmniej 5 znaków.'
    }

    setValidationErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return

    setIsSubmitting(true)
    setError(null)

    try {
      const res = await hubApi.threads.create(
        subject.trim(),
        message.trim(),
        initialRelatedIdeaId || null,
      )

      if (res.error) {
        if (res.error.status === 401) {
          login()
          return
        }
        setError(res.error.message || 'Nie udało się wysłać wiadomości. Spróbuj ponownie.')
      } else if (res.value) {
        const threadId = res.value.id
        setSubject('')
        setMessage('')
        setValidationErrors({})
        onClose()
        if (onThreadCreated) {
          onThreadCreated(threadId)
        } else {
          navigate(`/wiadomosci/${threadId}`)
        }
      }
    } catch {
      setError('Wystąpił błąd sieci. Spróbuj ponownie za chwilę.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleClose = () => {
    if (!isSubmitting) {
      setError(null)
      setValidationErrors({})
      onClose()
    }
  }

  return (
    <Modal
      opened={opened}
      onClose={handleClose}
      title="Napisz nową wiadomość do ROPS"
      size="lg"
      radius="md"
      trapFocus
      returnFocus
      closeOnEscape
      styles={{
        title: { fontSize: '1.35rem', fontWeight: 700 },
        header: { paddingBottom: 16 },
      }}
    >
      <form onSubmit={handleSubmit} noValidate>
        <Stack gap="lg">
          <Text size="md" c="dimmed" style={{ fontSize: '1.05rem', lineHeight: 1.5 }}>
            Masz pytanie do pracowników Regionalnego Ośrodka Polityki Społecznej w Krakowie?
            Wpisz poniżej temat i treść, a nasi specjaliści odpowiedzą bezpośrednio w tym wątku.
          </Text>

          {initialRelatedIdeaId && (
            <Alert color="blue" title="Dotyczy pomysłu" radius="md">
              <Text size="sm">
                Wiadomość zostanie powiązana z Twoim pomysłem o identyfikatorze:{' '}
                <strong>{initialRelatedIdeaId}</strong>
              </Text>
            </Alert>
          )}

          {error && (
            <Alert
              icon={<IconAlertCircle size={20} />}
              color="red"
              title="Błąd wysyłania"
              radius="md"
              role="alert"
            >
              {error}
            </Alert>
          )}

          <TextInput
            label="Temat rozmowy"
            placeholder="np. Pytanie o wsparcie seniorów w gminie Wieliczka"
            required
            value={subject}
            onChange={(e) => {
              setSubject(e.currentTarget.value)
              if (validationErrors.subject) {
                setValidationErrors((prev) => ({ ...prev, subject: undefined }))
              }
            }}
            error={validationErrors.subject}
            size="md"
            styles={{
              label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 6 },
              input: { fontSize: '1.05rem', height: 46 },
            }}
          />

          <Textarea
            label="Treść wiadomości"
            placeholder="Opisz dokładnie, w czym możemy Ci pomóc lub jakie masz wątpliwości..."
            required
            minRows={5}
            value={message}
            onChange={(e) => {
              setMessage(e.currentTarget.value)
              if (validationErrors.message) {
                setValidationErrors((prev) => ({ ...prev, message: undefined }))
              }
            }}
            error={validationErrors.message}
            size="md"
            styles={{
              label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 6 },
              input: { fontSize: '1.05rem', lineHeight: 1.5 },
            }}
          />

          <Group justify="flex-end" gap="md" mt="sm">
            <Button
              variant="default"
              onClick={handleClose}
              size="lg"
              disabled={isSubmitting}
              styles={{ root: { fontSize: '1rem' } }}
            >
              Anuluj
            </Button>

            <Button
              type="submit"
              size="lg"
              color="blue"
              loading={isSubmitting}
              leftSection={<IconSend size={20} aria-hidden="true" />}
              styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
            >
              Wyślij wiadomość
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
