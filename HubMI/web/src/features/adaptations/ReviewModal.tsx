import { useState } from 'react'
import { Alert, Button, Group, Modal, Stack, Text, Textarea } from '@mantine/core'
import type { AdaptationJs } from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { describeApiError } from '../../api/errors'

export interface ReviewModalProps {
  /** The plan being decided on and the decision (`APPROVED` or `REJECTED`); `null` keeps the modal closed. */
  review: { adaptation: AdaptationJs; decision: 'APPROVED' | 'REJECTED' } | null
  onClose: () => void
  onReviewed: (updated: AdaptationJs) => void
  /** The plan changed under the admin (HTTP 409): the list has to be reloaded. */
  onConflict: () => void
}

const MAX_COMMENT_LENGTH = 1000

/** A review is final, so it always goes through this confirmation; rejecting needs a reason for the author. */
export function ReviewModal({ review, onClose, onReviewed, onConflict }: ReviewModalProps) {
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  if (!review) return null
  const rejecting = review.decision === 'REJECTED'

  const close = () => {
    setComment('')
    setError(null)
    onClose()
  }

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = comment.trim()
    if (rejecting && trimmed === '') {
      setError('Napisz autorowi, dlaczego odrzucasz plan.')
      return
    }
    setSending(true)
    setError(null)
    const result = await hubApi.admin.reviewAdaptation(review.adaptation.id, review.decision, trimmed || undefined)
    setSending(false)
    if (result.value) {
      setComment('')
      onReviewed(result.value)
      return
    }
    if (result.error?.status === 409) {
      close()
      onConflict()
      return
    }
    setError(result.error ? describeApiError(result.error) : 'Nie udało się zapisać decyzji.')
  }

  return (
    <Modal
      opened
      onClose={close}
      title={rejecting ? 'Odrzuć plan adaptacji' : 'Zatwierdź plan adaptacji'}
      size="lg"
      trapFocus
      returnFocus
    >
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          <Text size="md">
            Plan dla innowacji <strong>{review.adaptation.innovationTitle}</strong>. Decyzji nie można potem zmienić.
          </Text>
          <Textarea
            label={rejecting ? 'Powód odrzucenia (zobaczy go autor)' : 'Komentarz dla autora (opcjonalnie)'}
            value={comment}
            onChange={(e) => setComment(e.currentTarget.value)}
            maxLength={MAX_COMMENT_LENGTH}
            minRows={3}
            autosize
            required={rejecting}
            error={rejecting ? error : undefined}
            data-autofocus
          />
          {error && !rejecting && (
            <Alert color="red" role="alert">
              {error}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={close} disabled={sending}>
              Anuluj
            </Button>
            <Button type="submit" color={rejecting ? 'red' : 'teal'} loading={sending}>
              {rejecting ? 'Odrzuć plan' : 'Zatwierdź plan'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
