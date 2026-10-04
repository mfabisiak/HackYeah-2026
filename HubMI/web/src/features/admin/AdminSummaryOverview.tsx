import {
  Alert,
  Button,
  Card,
  Group,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconAlertTriangle,
  IconArrowRight,
  IconCheck,
  IconFilePlus,
  IconHeartHandshake,
  IconMessageDots,
  IconRefresh,
  IconSearch,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { formatApiError } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import { clearAuthSession, getAccessToken } from '../../auth/keycloak'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { pluralizeSprawy } from './constants'
import type { AdminSummaryJs } from 'hubmi-client'

export interface AdminSummaryOverviewProps {
  onNavigateTab: (tab: string) => void
  /** Whether the shortcut to the trends is offered; trends are for the admin only. */
  canSeeTrends?: boolean
}

export function AdminSummaryOverview({ onNavigateTab, canSeeTrends = true }: AdminSummaryOverviewProps) {
  const { login } = useAuth()
  const [summary, setSummary] = useState<AdminSummaryJs | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isAuthExpired, setIsAuthExpired] = useState(false)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  useEffect(() => {
    let cancelled = false

    const fetchSummary = async () => {
      try {
        let loadedSummary: AdminSummaryJs | null = null
        let fetchError: unknown = null

        // 1. Try typed hubApi method if available
        if (typeof hubApi.admin?.summary === 'function') {
          try {
            const res = await hubApi.admin.summary()
            if (res.error) {
              fetchError = res.error
            } else if (res.value) {
              loadedSummary = res.value
            }
          } catch (e) {
            fetchError = e
          }
        }

        // 2. HTTP fallback in case client bundle is cached or missing summary method
        if (!loadedSummary && !fetchError) {
          const token = getAccessToken()
          const resp = await fetch('/api/admin/summary', {
            headers: {
              ...(token ? { Authorization: `Bearer ${token}` } : {}),
              Accept: 'application/json',
            },
          })
          if (resp.status === 401) {
            clearAuthSession()
            setIsAuthExpired(true)
            setError('Twoja sesja wygasła lub klucze autoryzacji uległy zmianie. Zaloguj się ponownie, aby uzyskać dostęp do panelu.')
            setIsLoading(false)
            return
          }
          if (resp.ok) {
            loadedSummary = (await resp.json()) as AdminSummaryJs
          } else {
            fetchError = { status: resp.status, message: 'Nie udało się pobrać danych podsumowania.' }
          }
        }

        if (cancelled) return

        if (fetchError) {
          const errObj = fetchError as { status?: number }
          if (errObj.status === 401) {
            clearAuthSession()
            setIsAuthExpired(true)
            setError('Twoja sesja wygasła lub klucze autoryzacji uległy zmianie. Zaloguj się ponownie, aby uzyskać dostęp do panelu.')
          } else {
            setError(formatApiError(fetchError))
          }
        } else if (loadedSummary) {
          setSummary(loadedSummary)
          setError(null)
          setIsAuthExpired(false)
        } else {
          setError('Nie udało się pobrać danych podsumowania administratora. Sprawdź połączenie z serwerem.')
        }
      } catch (err) {
        if (!cancelled) {
          setError(formatApiError(err))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchSummary()

    return () => {
      cancelled = true
    }
  }, [refreshTrigger])

  const handleRefresh = () => {
    setIsLoading(true)
    setError(null)
    setIsAuthExpired(false)
    setRefreshTrigger((prev) => prev + 1)
  }

  if (isLoading) {
    return <LoadingState message="Wczytuję podsumowanie zadań administratora..." minHeight={200} />
  }

  if (error) {
    return (
      <Stack gap="md">
        <ErrorAlert message={error} onRetry={isAuthExpired ? undefined : handleRefresh} />
        {isAuthExpired && (
          <Group justify="center">
            <Button size="md" color="blue" onClick={login}>
              Zaloguj się ponownie
            </Button>
          </Group>
        )}
      </Stack>
    )
  }

  const submittedIdeas = summary?.submittedIdeas ?? 0
  const pendingTestRequests = summary?.pendingTestRequests ?? 0
  const pendingThreads = summary?.pendingThreads ?? 0
  const unmatchedNeeds = summary?.unmatchedNeedsThisWeek ?? 0

  const totalUrgent = submittedIdeas + pendingTestRequests + pendingThreads

  return (
    <Stack gap="xl">
      {/* Alert banner summarizing items needing action */}
      {totalUrgent > 0 ? (
        <Alert
          color="orange"
          icon={<IconAlertTriangle size={24} aria-hidden="true" />}
          title="Zadania wymagające uwagi"
          radius="md"
          styles={{
            title: { fontSize: '1.25rem', fontWeight: 700 },
            message: { fontSize: '1.05rem', lineHeight: 1.6 },
          }}
        >
          Masz łącznie <strong>{totalUrgent}</strong> {pluralizeSprawy(totalUrgent).noun}{' '}
          {pluralizeSprawy(totalUrgent).adjective} na reakcję pracownika ROPS (nowe pomysły,
          zgłoszenia do testów lub wiadomości). Poniżej znajdziesz bezpośrednie przejścia do każdej
          z kolejek.
        </Alert>
      ) : (
        <Alert
          color="teal"
          icon={<IconCheck size={24} aria-hidden="true" />}
          title="Wszystkie sprawy są na bieżąco obsłużone"
          radius="md"
          styles={{
            title: { fontSize: '1.2rem', fontWeight: 700 },
            message: { fontSize: '1.05rem' },
          }}
        >
          Brak zaległych zadań wymagających natychmiastowej weryfikacji. Dobra robota!
        </Alert>
      )}

      {/* 4 Large Senior-Friendly Metric Cards */}
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="lg">
        {/* Card 1: Submitted Ideas */}
        <Card
          withBorder
          padding="xl"
          radius="md"
          style={{
            borderLeft: submittedIdeas > 0 ? '6px solid var(--mantine-color-blue-filled)' : undefined,
            backgroundColor: submittedIdeas > 0 ? 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))' : undefined,
          }}
        >
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase" style={{ fontSize: '0.9rem' }}>
                  Nowe pomysły
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.5rem', fontWeight: 800 }}>
                  {submittedIdeas}
                </Title>
              </div>
              <ThemeIcon size={52} radius="md" color="blue" variant="light">
                <IconFilePlus size={28} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem' }}>
              Pomysły innowacji zgłoszone przez mieszkańców Małopolski, oczekujące na moderację.
            </Text>
            <Button
              variant="light"
              color="blue"
              size="md"
              fullWidth
              onClick={() => onNavigateTab('pomysly')}
              rightSection={<IconArrowRight size={18} aria-hidden="true" />}
              styles={{
                root: {
                  fontSize: '1rem',
                  fontWeight: 600,
                  paddingInline: 12,
                  whiteSpace: 'normal',
                  height: 'auto',
                  minHeight: 44,
                  paddingBlock: 8,
                },
              }}
            >
              Rozpatrz pomysły
            </Button>
          </Stack>
        </Card>

        {/* Card 2: Test Requests */}
        <Card
          withBorder
          padding="xl"
          radius="md"
          style={{
            borderLeft: pendingTestRequests > 0 ? '6px solid var(--mantine-color-teal-filled)' : undefined,
            backgroundColor: pendingTestRequests > 0 ? 'light-dark(var(--mantine-color-teal-0), var(--mantine-color-dark-6))' : undefined,
          }}
        >
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase" style={{ fontSize: '0.9rem' }}>
                  Zgłoszenia do testów
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.5rem', fontWeight: 800 }}>
                  {pendingTestRequests}
                </Title>
              </div>
              <ThemeIcon size={52} radius="md" color="teal" variant="light">
                <IconHeartHandshake size={28} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem' }}>
              Obywatele i seniorzy deklarujący chęć udziału w testowaniu rozwiązań.
            </Text>
            <Button
              variant="light"
              color="teal"
              size="md"
              fullWidth
              onClick={() => onNavigateTab('testy')}
              rightSection={<IconArrowRight size={18} aria-hidden="true" />}
              styles={{
                root: {
                  fontSize: '1rem',
                  fontWeight: 600,
                  paddingInline: 12,
                  whiteSpace: 'normal',
                  height: 'auto',
                  minHeight: 44,
                  paddingBlock: 8,
                },
              }}
            >
              Kolejka testerów
            </Button>
          </Stack>
        </Card>

        {/* Card 3: Pending Messages */}
        <Card
          withBorder
          padding="xl"
          radius="md"
          style={{
            borderLeft: pendingThreads > 0 ? '6px solid var(--mantine-color-indigo-filled)' : undefined,
            backgroundColor: pendingThreads > 0 ? 'light-dark(var(--mantine-color-indigo-0), var(--mantine-color-dark-6))' : undefined,
          }}
        >
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase" style={{ fontSize: '0.9rem' }}>
                  Wiadomości ROPS
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.5rem', fontWeight: 800 }}>
                  {pendingThreads}
                </Title>
              </div>
              <ThemeIcon size={52} radius="md" color="indigo" variant="light">
                <IconMessageDots size={28} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem' }}>
              Wątki dialogu wymagające odpowiedzi ze strony pracownika lub eksperta ROPS.
            </Text>
            <Button
              variant="light"
              color="indigo"
              size="md"
              fullWidth
              component="a"
              href="/wiadomosci"
              rightSection={<IconArrowRight size={18} aria-hidden="true" />}
              styles={{
                root: {
                  fontSize: '1rem',
                  fontWeight: 600,
                  paddingInline: 12,
                  whiteSpace: 'normal',
                  height: 'auto',
                  minHeight: 44,
                  paddingBlock: 8,
                },
              }}
            >
              Otwórz skrzynkę
            </Button>
          </Stack>
        </Card>

        {/* Card 4: Unmatched Needs This Week */}
        <Card withBorder padding="xl" radius="md">
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase" style={{ fontSize: '0.9rem' }}>
                  Luki w bazie (ten tydz.)
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.5rem', fontWeight: 800 }}>
                  {unmatchedNeeds}
                </Title>
              </div>
              <ThemeIcon size={52} radius="md" color="gray" variant="light">
                <IconSearch size={28} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem' }}>
              Wyszukiwania mieszkańców, dla których nie znaleziono gotowego rozwiązania.
            </Text>
            {canSeeTrends && (
              <Button
                variant="light"
                color="orange"
                size="md"
                fullWidth
                onClick={() => onNavigateTab('trendy')}
                rightSection={<IconArrowRight size={18} aria-hidden="true" />}
                styles={{
                  root: {
                    fontSize: '1rem',
                    fontWeight: 600,
                    paddingInline: 12,
                    whiteSpace: 'normal',
                    height: 'auto',
                    minHeight: 44,
                    paddingBlock: 8,
                  },
                }}
              >
                Analiza trendów i luk
              </Button>
            )}
          </Stack>
        </Card>
      </SimpleGrid>

      <Group justify="flex-end">
        <Button
          variant="subtle"
          color="gray"
          size="md"
          onClick={handleRefresh}
          leftSection={<IconRefresh size={18} aria-hidden="true" />}
        >
          Odśwież wskaźniki
        </Button>
      </Group>
    </Stack>
  )
}
