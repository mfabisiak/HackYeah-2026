import { useEffect, useState } from 'react'
import { Button, Stack, Text } from '@mantine/core'
import { IconSparkles } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import type { AdaptationJs } from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { describeApiError } from '../../api/errors'
import { RequireAuth } from '../../auth/RequireAuth'
import { EmptyState } from '../../components/EmptyState'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { AccessiblePagination } from '../../components/Pagination'
import { PageHeader } from '../../components/PageHeader'
import { AdaptationAccordion } from './AdaptationAccordion'

const PAGE_SIZE = 10

function MyAdaptationsContent() {
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<AdaptationJs[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    void hubApi.adaptations.mine(page, PAGE_SIZE).then((result) => {
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
  }, [page, attempt])

  return (
    <Stack gap="xl">
      <PageHeader
        title="Moje plany adaptacji"
        subtitle="Plany wdrożenia innowacji w Twojej instytucji, które przygotował asystent AI. Każdy czeka na przegląd pracownika ROPS."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje plany adaptacji' }]}
      />

      {loading && <LoadingState message="Wczytuję Twoje plany…" />}

      {error && (
        <ErrorAlert
          message={error}
          onRetry={() => {
            setLoading(true)
            setAttempt((n) => n + 1)
          }}
        />
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title="Nie masz jeszcze żadnego planu"
          description="Otwórz dowolną innowację w bazie i wybierz „Dostosuj do mojej instytucji”, a asystent przygotuje plan wdrożenia."
          action={
            <Button component={Link} to="/innowacje" leftSection={<IconSparkles size={18} aria-hidden="true" />}>
              Przejdź do bazy innowacji
            </Button>
          }
        />
      )}

      {items.length > 0 && (
        <Stack gap="md">
          <Text size="md" aria-live="polite">
            Liczba planów: {total}
          </Text>
          <AdaptationAccordion items={items} />
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
    </Stack>
  )
}

export function MyAdaptationsPage() {
  return (
    <RequireAuth>
      <MyAdaptationsContent />
    </RequireAuth>
  )
}
