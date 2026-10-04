import { useEffect, useState } from 'react'
import { Alert, Button, Group, SegmentedControl, Stack, Text, Title } from '@mantine/core'
import { IconCheck, IconX } from '@tabler/icons-react'
import type { AdaptationJs } from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { describeApiError } from '../../api/errors'
import { EmptyState } from '../../components/EmptyState'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { AccessiblePagination } from '../../components/Pagination'
import { AdaptationAccordion } from './AdaptationAccordion'
import { ADAPTATION_STATUS_FILTERS } from './constants'
import { ReviewModal, type ReviewModalProps } from './ReviewModal'

const PAGE_SIZE = 10

/** The plans the Middleman wrote, waiting for an admin to approve or reject them; one tab of the admin panel. */
export function AdaptationsReviewQueue() {
  const [status, setStatus] = useState<string>('PENDING_REVIEW')
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<AdaptationJs[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [review, setReview] = useState<ReviewModalProps['review']>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    void hubApi.admin.adaptations(status, page, PAGE_SIZE).then((result) => {
      if (cancelled) return
      setLoading(false)
      if (result.value) {
        setItems(Array.from(result.value.items))
        setTotal(result.value.total)
        setError(null)
      } else {
        setError(result.error ? describeApiError(result.error) : 'Nie udało się pobrać planów.')
      }
    })
    return () => {
      cancelled = true
    }
  }, [status, page, attempt])

  const reload = () => {
    setLoading(true)
    setAttempt((n) => n + 1)
  }

  return (
    <Stack gap="xl">
      <Stack gap={2}>
        <Title order={2} size="h3" style={{ fontSize: '1.4rem', fontWeight: 700 }}>
          Plany adaptacji do przeglądu
        </Title>
        <Text size="md" c="dimmed" style={{ fontSize: '1.05rem' }}>
          Plany wdrożenia innowacji, które przygotował asystent AI dla instytucji. Zanim autor dostanie je jako sprawdzone,
          ktoś z ROPS musi je zatwierdzić albo odrzucić.
        </Text>
      </Stack>

      <SegmentedControl
        aria-label="Filtr statusu planów"
        value={status}
        onChange={(value) => {
          setStatus(value)
          setPage(0)
          setLoading(true)
        }}
        data={ADAPTATION_STATUS_FILTERS.map((filter) => ({ value: filter.value, label: filter.label }))}
        size="md"
      />

      <div role="status" aria-live="polite">
        {notice && (
          <Alert color="teal" withCloseButton onClose={() => setNotice(null)}>
            {notice}
          </Alert>
        )}
      </div>

      {loading && <LoadingState message="Wczytuję plany…" />}
      {error && <ErrorAlert message={error} onRetry={reload} />}

      {!loading && !error && items.length === 0 && (
        <EmptyState title="Brak planów w tym widoku" description="Gdy ktoś poprosi asystenta o plan, pojawi się tutaj." />
      )}

      {items.length > 0 && (
        <Stack gap="md">
          <Text size="md" aria-live="polite">
            Liczba planów: {total}
          </Text>
          <AdaptationAccordion
            items={items}
            actionsFor={(adaptation) =>
              adaptation.status === 'PENDING_REVIEW' ? (
                <Group gap="md">
                  <Button
                    color="teal"
                    leftSection={<IconCheck size={18} aria-hidden="true" />}
                    onClick={() => setReview({ adaptation, decision: 'APPROVED' })}
                  >
                    Zatwierdź
                  </Button>
                  <Button
                    color="red"
                    variant="light"
                    leftSection={<IconX size={18} aria-hidden="true" />}
                    onClick={() => setReview({ adaptation, decision: 'REJECTED' })}
                  >
                    Odrzuć
                  </Button>
                </Group>
              ) : null
            }
          />
          <AccessiblePagination
            total={Math.ceil(total / PAGE_SIZE)}
            value={page + 1}
            onChange={(value) => {
              setLoading(true)
              setPage(value - 1)
            }}
            totalCount={total}
            itemsPerPage={PAGE_SIZE}
          />
        </Stack>
      )}

      <ReviewModal
        review={review}
        onClose={() => setReview(null)}
        onReviewed={(updated) => {
          setReview(null)
          setNotice(`Zapisano decyzję dla planu „${updated.innovationTitle}”.`)
          reload()
        }}
        onConflict={() => {
          setNotice('Ktoś już zmienił status tego planu. Odświeżyliśmy listę.')
          reload()
        }}
      />
    </Stack>
  )
}
