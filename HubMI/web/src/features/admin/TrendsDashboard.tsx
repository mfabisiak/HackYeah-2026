import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Progress,
  Select,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Table,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconAlertCircle,
  IconCalendar,
  IconChartBar,
  IconLayersLinked,
  IconMapPin,
  IconMinus,
  IconPlus,
  IconRefresh,
  IconSearch,
  IconShieldCheck,
  IconTable,
  IconTrendingDown,
  IconTrendingUp,
} from '@tabler/icons-react'
import { useEffect, useId, useState } from 'react'
import { formatApiError } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import { clearAuthSession, getAccessToken } from '../../auth/keycloak'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { SOCIAL_AREA_NAMES } from '../knowledge/constants'
import type { AreaTrendJs, TrendsJs } from 'hubmi-client'

export interface TrendsDashboardProps {
  onNavigateTab?: (tab: string, subTab?: string) => void
}

const MONTHS_OPTIONS = [
  { value: '1', label: 'Ostatni miesiąc (1 mies.)' },
  { value: '3', label: 'Ostatni kwartał (3 mies.)' },
  { value: '6', label: 'Ostatnie półrocze (6 mies. - domyślnie)' },
  { value: '12', label: 'Ostatni rok (12 mies.)' },
  { value: '24', label: 'Ostatnie 2 lata (24 mies.)' },
]

export function TrendsDashboard({ onNavigateTab }: TrendsDashboardProps) {
  const { login } = useAuth()
  const viewModeId = useId()

  const [months, setMonths] = useState<string>('6')
  const [trends, setTrends] = useState<TrendsJs | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isAuthExpired, setIsAuthExpired] = useState(false)
  const [refreshTrigger, setRefreshTrigger] = useState(0)
  const [viewMode, setViewMode] = useState<'both' | 'table'>('both')

  useEffect(() => {
    let cancelled = false

    const fetchTrends = async () => {
      setIsLoading(true)
      setError(null)
      setIsAuthExpired(false)

      try {
        let loadedTrends: TrendsJs | null = null
        let fetchError: unknown = null
        const monthsNum = parseInt(months, 10) || 6

        // 1. Try typed hubApi method if available
        if (typeof hubApi.admin?.trends === 'function') {
          try {
            const res = await hubApi.admin.trends(monthsNum)
            if (res.error) {
              fetchError = res.error
            } else if (res.value) {
              loadedTrends = res.value
            }
          } catch (e) {
            fetchError = e
          }
        }

        // 2. HTTP fallback in case client bundle is cached or missing trends method
        if (!loadedTrends && !fetchError) {
          const token = getAccessToken()
          const resp = await fetch(`/api/admin/trends?months=${monthsNum}`, {
            headers: {
              ...(token ? { Authorization: `Bearer ${token}` } : {}),
              Accept: 'application/json',
            },
          })
          if (resp.status === 401) {
            clearAuthSession()
            setIsAuthExpired(true)
            setError('Twoja sesja wygasła. Zaloguj się ponownie, aby uzyskać dostęp do analityki trendów.')
            setIsLoading(false)
            return
          }
          if (resp.ok) {
            loadedTrends = (await resp.json()) as TrendsJs
          } else {
            fetchError = { status: resp.status, message: 'Nie udało się pobrać danych trendów społecznych.' }
          }
        }

        if (cancelled) return

        if (fetchError) {
          const errObj = fetchError as { status?: number }
          if (errObj.status === 401) {
            clearAuthSession()
            setIsAuthExpired(true)
            setError('Twoja sesja wygasła. Zaloguj się ponownie, aby uzyskać dostęp do analityki trendów.')
          } else {
            setError(formatApiError(fetchError))
          }
        } else if (loadedTrends) {
          setTrends(loadedTrends)
          setError(null)
          setIsAuthExpired(false)
        } else {
          setError('Nie udało się pobrać danych trendów społecznych. Sprawdź połączenie z serwerem.')
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

    void fetchTrends()

    return () => {
      cancelled = true
    }
  }, [months, refreshTrigger])

  const handleRefresh = () => {
    setRefreshTrigger((prev) => prev + 1)
  }

  if (isLoading && !trends) {
    return <LoadingState message="Wczytuję analizę trendów i potrzeb społecznych Małopolski..." minHeight={300} />
  }

  if (error && !trends) {
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

  const byArea = trends?.byArea ?? []
  const byMunicipality = trends?.byMunicipality ?? []
  const series = trends?.series ?? []
  const topUnmatchedTerms = trends?.topUnmatchedTerms ?? []
  const privacyThreshold = trends?.privacyThreshold ?? 3
  const unmatchedNeeds = trends?.unmatchedNeeds ?? 0

  const totalNeedsInPeriod = byArea.reduce((sum, item) => sum + item.count, 0)
  const topArea = byArea.length > 0
    ? [...byArea].sort((a, b) => b.count - a.count)[0]
    : null

  // Sort areas by count descending for visual presentation
  const sortedAreas = [...byArea].sort((a, b) => b.count - a.count)
  const maxAreaCount = sortedAreas.length > 0 ? Math.max(...sortedAreas.map((a) => a.count), 1) : 1

  // Sort municipalities by count descending
  const sortedMunicipalities = [...byMunicipality].sort((a, b) => b.count - a.count)
  const maxMunicipalityCount = sortedMunicipalities.length > 0 ? Math.max(...sortedMunicipalities.map((m) => m.count), 1) : 1

  // Max value in timeline series
  const maxSeriesCount = series.length > 0 ? Math.max(...series.map((s) => s.count), 1) : 1

  const formatAreaName = (areaCode: string) => {
    return SOCIAL_AREA_NAMES[areaCode] ?? areaCode
  }

  const calculateTrendDelta = (item: AreaTrendJs) => {
    const diff = item.count - item.previousCount
    if (item.previousCount === 0) {
      return { diff, percent: item.count > 0 ? 100 : 0, isNew: true }
    }
    const percent = Math.round((diff / item.previousCount) * 100)
    return { diff, percent, isNew: false }
  }

  return (
    <Stack gap="xl">
      {/* Header and Controls */}
      <Card withBorder padding="lg" radius="md">
        <Stack gap="md">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <div>
              <Title order={2} size="h3" style={{ fontSize: '1.4rem', fontWeight: 700 }}>
                Analiza trendów i diagnoza potrzeb społecznych
              </Title>
              <Text size="sm" c="dimmed" style={{ fontSize: '1rem', marginTop: 4 }}>
                Agregacja zgłaszanych przez mieszkańców wyzwań w gminach Małopolski i obszarach tematycznych.
              </Text>
            </div>
            <Group gap="sm" align="flex-end">
              <div>
                <Select
                  label="Okres analizy"
                  data={MONTHS_OPTIONS}
                  value={months}
                  onChange={(val) => val && setMonths(val)}
                  leftSection={<IconCalendar size={18} aria-hidden="true" />}
                  size="sm"
                  style={{ minWidth: 260 }}
                />
              </div>

              <div>
                <Text component="label" htmlFor={viewModeId} size="xs" fw={700} c="dimmed" mb={4}>
                  Format prezentacji:
                </Text>
                <SegmentedControl
                  id={viewModeId}
                  value={viewMode}
                  onChange={(val) => setViewMode(val as 'both' | 'table')}
                  data={[
                    {
                      value: 'both',
                      label: (
                        <Group gap={6} wrap="nowrap">
                          <IconChartBar size={16} aria-hidden="true" />
                          <span>Wykres i tabela</span>
                        </Group>
                      ),
                    },
                    {
                      value: 'table',
                      label: (
                        <Group gap={6} wrap="nowrap">
                          <IconTable size={16} aria-hidden="true" />
                          <span>Tylko tabela</span>
                        </Group>
                      ),
                    },
                  ]}
                  size="sm"
                  aria-label="Wybierz format prezentacji danych"
                />
              </div>

              <Button
                variant="light"
                color="blue"
                size="sm"
                onClick={handleRefresh}
                loading={isLoading}
                leftSection={<IconRefresh size={16} aria-hidden="true" />}
                aria-label="Odśwież dane trendów"
              >
                Odśwież
              </Button>
            </Group>
          </Group>

          {/* Privacy protection notice */}
          <Alert
            color="indigo"
            icon={<IconShieldCheck size={22} aria-hidden="true" />}
            radius="md"
            styles={{
              title: { fontSize: '1rem', fontWeight: 700 },
              message: { fontSize: '0.92rem', lineHeight: 1.5 },
            }}
            title="Ochrona prywatności i agregacja regionalna (K-Anonymity)"
          >
            Dane są w pełni zanonimizowane. Zgodnie z zasadą prywatności (próg: minimum{' '}
            <strong>{privacyThreshold} zgłoszenia</strong>), gminy i słowa kluczowe z pojedynczymi
            zapytaniami są wyłączane ze statystyk jednostkowych, aby uniemożliwić deanonimizację mieszkańców.
          </Alert>
        </Stack>
      </Card>

      {/* Top Metric KPI Cards */}
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="lg">
        {/* KPI 1: Total Needs */}
        <Card withBorder padding="lg" radius="md">
          <Group justify="space-between" align="flex-start">
            <div>
              <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                Zgłoszone potrzeby
              </Text>
              <Title order={3} size="h1" style={{ fontSize: '2.2rem', fontWeight: 800, marginTop: 4 }}>
                {totalNeedsInPeriod}
              </Title>
            </div>
            <ThemeIcon size={46} radius="md" color="blue" variant="light">
              <IconLayersLinked size={26} aria-hidden="true" />
            </ThemeIcon>
          </Group>
          <Text size="sm" c="dimmed" mt="xs" style={{ fontSize: '0.92rem' }}>
            Łączna liczba zidentyfikowanych potrzeb mieszkańców w wybranym okresie {months} mies.
          </Text>
        </Card>

        {/* KPI 2: Leading Area */}
        <Card withBorder padding="lg" radius="md">
          <Group justify="space-between" align="flex-start">
            <div>
              <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                Wiodący obszar wyzwań
              </Text>
              <Title order={3} size="h2" style={{ fontSize: '1.4rem', fontWeight: 800, marginTop: 4 }}>
                {topArea ? formatAreaName(topArea.area) : 'Brak danych'}
              </Title>
            </div>
            <ThemeIcon size={46} radius="md" color="teal" variant="light">
              <IconTrendingUp size={26} aria-hidden="true" />
            </ThemeIcon>
          </Group>
          {topArea && (
            <Text size="sm" c="dimmed" mt="xs" style={{ fontSize: '0.92rem' }}>
              <strong>{topArea.count}</strong> zgłoszeń (
              {totalNeedsInPeriod > 0 ? Math.round((topArea.count / totalNeedsInPeriod) * 100) : 0}% wszystkich potrzeb)
            </Text>
          )}
        </Card>

        {/* KPI 3: Unmatched Needs */}
        <Card withBorder padding="lg" radius="md">
          <Group justify="space-between" align="flex-start">
            <div>
              <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                Luki w bazie innowacji
              </Text>
              <Title order={3} size="h1" style={{ fontSize: '2.2rem', fontWeight: 800, marginTop: 4 }}>
                {unmatchedNeeds}
              </Title>
            </div>
            <ThemeIcon size={46} radius="md" color="orange" variant="light">
              <IconSearch size={26} aria-hidden="true" />
            </ThemeIcon>
          </Group>
          <Text size="sm" c="dimmed" mt="xs" style={{ fontSize: '0.92rem' }}>
            Zapytania mieszkańców, dla których nie znaleziono gotowego rozwiązania w bibliotece.
          </Text>
        </Card>

        {/* KPI 4: Privacy Threshold */}
        <Card withBorder padding="lg" radius="md">
          <Group justify="space-between" align="flex-start">
            <div>
              <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                Próg anonimizacji
              </Text>
              <Title order={3} size="h1" style={{ fontSize: '2.2rem', fontWeight: 800, marginTop: 4 }}>
                ≥ {privacyThreshold}
              </Title>
            </div>
            <ThemeIcon size={46} radius="md" color="indigo" variant="light">
              <IconShieldCheck size={26} aria-hidden="true" />
            </ThemeIcon>
          </Group>
          <Text size="sm" c="dimmed" mt="xs" style={{ fontSize: '0.92rem' }}>
            Minimalna liczba zgłoszeń wymagana do ujawnienia statystyki dla gminy lub frazy.
          </Text>
        </Card>
      </SimpleGrid>

      {/* Section 1: Needs by Social Area */}
      <Card withBorder padding="xl" radius="md">
        <Stack gap="lg">
          <div>
            <Group justify="space-between" align="baseline">
              <Title order={3} size="h3" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                1. Rozkład potrzeb według obszarów z Mapy Wyzwań Społecznych
              </Title>
              <Badge color="blue" variant="outline" size="lg">
                {byArea.length} obszarów
              </Badge>
            </Group>
            {topArea && (
              <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem', marginTop: 4 }}>
                W analizowanym okresie najwięcej zgłoszeń odnotowano w obszarze{' '}
                <strong>„{formatAreaName(topArea.area)}”</strong> (łącznie {topArea.count} zgłoszeń,{' '}
                {topArea.previousCount > 0 ? (
                  <span>
                    poprzednio {topArea.previousCount},{' '}
                    {calculateTrendDelta(topArea).diff >= 0 ? '+' : ''}
                    {calculateTrendDelta(topArea).percent}% zmiana
                  </span>
                ) : (
                  <span>nowo zaobserwowany trend</span>
                )}
                ).
              </Text>
            )}
          </div>

          {/* Visual Bar Chart (Accessible) */}
          {viewMode === 'both' && (
            <Stack gap="md" role="region" aria-label="Wizualizacja potrzeb według obszarów">
              {sortedAreas.map((item) => {
                const percentage = Math.round((item.count / maxAreaCount) * 100)
                const shareOfTotal = totalNeedsInPeriod > 0 ? Math.round((item.count / totalNeedsInPeriod) * 100) : 0
                const delta = calculateTrendDelta(item)

                return (
                  <div key={item.area}>
                    <Group justify="space-between" mb={4}>
                      <Group gap="xs">
                        <Text fw={600} size="sm" style={{ fontSize: '1rem' }}>
                          {formatAreaName(item.area)}
                        </Text>
                        <Badge size="sm" variant="light" color="gray">
                          {shareOfTotal}% całości
                        </Badge>
                      </Group>
                      <Group gap="sm">
                        <Text fw={700} size="sm" style={{ fontSize: '1rem' }}>
                          {item.count} {item.count === 1 ? 'zgłoszenie' : 'zgłoszeń'}
                        </Text>
                        {delta.diff > 0 ? (
                          <Badge
                            color="teal"
                            variant="light"
                            size="md"
                            leftSection={<IconTrendingUp size={14} aria-hidden="true" />}
                            aria-label={`Wzrost o ${delta.percent}% względem poprzedniego okresu`}
                          >
                            +{delta.percent}%
                          </Badge>
                        ) : delta.diff < 0 ? (
                          <Badge
                            color="blue"
                            variant="light"
                            size="md"
                            leftSection={<IconTrendingDown size={14} aria-hidden="true" />}
                            aria-label={`Spadek o ${Math.abs(delta.percent)}% względem poprzedniego okresu`}
                          >
                            {delta.percent}%
                          </Badge>
                        ) : (
                          <Badge
                            color="gray"
                            variant="light"
                            size="md"
                            leftSection={<IconMinus size={14} aria-hidden="true" />}
                            aria-label="Liczba zgłoszeń bez zmian względem poprzedniego okresu"
                          >
                            0%
                          </Badge>
                        )}
                      </Group>
                    </Group>
                    <Progress
                      value={percentage}
                      color="blue"
                      size="lg"
                      radius="xl"
                      aria-label={`${formatAreaName(item.area)}: ${item.count} zgłoszeń`}
                      striped={delta.diff > 0}
                    />
                  </div>
                )
              })}
            </Stack>
          )}

          {/* Fully Accessible Data Table */}
          <div style={{ overflowX: 'auto' }}>
            <Table
              striped
              highlightOnHover
              withTableBorder
              withColumnBorders
              aria-label="Tabela potrzeb według obszarów społecznych"
            >
              <caption style={{ textAlign: 'left', padding: '8px 0', fontWeight: 600, color: 'light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-2))' }}>
                Tabela 1: Liczba zgłoszonych potrzeb per obszar wyzwań wraz z dynamiką zmian
              </caption>
              <Table.Thead style={{ backgroundColor: 'light-dark(var(--mantine-color-gray-1), var(--mantine-color-dark-6))' }}>
                <Table.Tr>
                  <Table.Th scope="col" style={{ padding: '12px', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Obszar wyzwań społecznych</Table.Th>
                  <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Liczba zgłoszeń (bieżący okres)</Table.Th>
                  <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Poprzedni okres</Table.Th>
                  <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Zmiana</Table.Th>
                  <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Udział w całości</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {sortedAreas.map((item) => {
                  const delta = calculateTrendDelta(item)
                  const share = totalNeedsInPeriod > 0 ? Math.round((item.count / totalNeedsInPeriod) * 100) : 0
                  return (
                    <Table.Tr key={item.area}>
                      <Table.Th scope="row" style={{ fontWeight: 600, padding: '12px', color: 'inherit' }}>
                        {formatAreaName(item.area)}
                      </Table.Th>
                      <Table.Td style={{ textAlign: 'right', padding: '12px', fontWeight: 700 }}>
                        {item.count}
                      </Table.Td>
                      <Table.Td style={{ textAlign: 'right', padding: '12px' }}>
                        {item.previousCount}
                      </Table.Td>
                      <Table.Td style={{ textAlign: 'right', padding: '12px' }}>
                        {delta.diff > 0 ? (
                          <span style={{ color: 'light-dark(var(--mantine-color-teal-7), var(--mantine-color-teal-4))', fontWeight: 700 }}>
                            +{delta.percent}% (+{delta.diff})
                          </span>
                        ) : delta.diff < 0 ? (
                          <span style={{ color: 'light-dark(var(--mantine-color-blue-7), var(--mantine-color-blue-4))', fontWeight: 700 }}>
                            {delta.percent}% ({delta.diff})
                          </span>
                        ) : (
                          <span style={{ color: 'light-dark(var(--mantine-color-gray-6), var(--mantine-color-dark-2))' }}>0% (0)</span>
                        )}
                      </Table.Td>
                      <Table.Td style={{ textAlign: 'right', padding: '12px' }}>
                        {share}%
                      </Table.Td>
                    </Table.Tr>
                  )
                })}
              </Table.Tbody>
            </Table>
          </div>
        </Stack>
      </Card>

      {/* Section 2: Timeline Series */}
      {series.length > 0 && (
        <Card withBorder padding="xl" radius="md">
          <Stack gap="lg">
            <Group justify="space-between" align="baseline">
              <Title order={3} size="h3" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                2. Dynamika zgłaszania potrzeb w czasie (miesiąc po miesiącu)
              </Title>
              <Badge color="teal" variant="outline" size="lg">
                {series.length} punktów czasowych
              </Badge>
            </Group>

            {/* Accessible Timeline Bar Chart */}
            {viewMode === 'both' && (
              <div
                role="region"
                aria-label="Wykres słupkowy dynamiki zgłoszeń w czasie"
                style={{
                  display: 'flex',
                  alignItems: 'flex-end',
                  justifyContent: 'space-between',
                  gap: '12px',
                  paddingTop: '24px',
                  paddingBottom: '8px',
                  minHeight: '220px',
                  borderBottom: '2px solid var(--mantine-color-gray-3)',
                }}
              >
                {series.map((point) => {
                  const heightPercent = Math.max(Math.round((point.count / maxSeriesCount) * 100), 10)
                  return (
                    <div
                      key={point.month}
                      style={{
                        flex: 1,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: '8px',
                      }}
                    >
                      <Text size="xs" fw={700} style={{ fontSize: '0.9rem' }}>
                        {point.count}
                      </Text>
                      <div
                        style={{
                          width: '100%',
                          maxWidth: '48px',
                          height: `${heightPercent * 1.5}px`,
                          backgroundColor: 'var(--mantine-color-teal-6)',
                          borderRadius: '6px 6px 0 0',
                          transition: 'height 0.3s ease',
                        }}
                        aria-label={`Miesiąc ${point.month}: ${point.count} zgłoszeń`}
                        role="img"
                      />
                      <Text size="xs" fw={600} c="dimmed" style={{ fontSize: '0.85rem' }}>
                        {point.month}
                      </Text>
                    </div>
                  )
                })}
              </div>
            )}

            {/* Timeline Data Table */}
            <div style={{ overflowX: 'auto' }}>
              <Table
                striped
                highlightOnHover
                withTableBorder
                withColumnBorders
                aria-label="Tabela dynamiki zgłoszeń w czasie"
              >
                <caption style={{ textAlign: 'left', padding: '8px 0', fontWeight: 600, color: 'light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-2))' }}>
                  Tabela 2: Rozkład liczby potrzeb w ujęciu chronologicznym
                </caption>
                <Table.Thead style={{ backgroundColor: 'light-dark(var(--mantine-color-gray-1), var(--mantine-color-dark-6))' }}>
                  <Table.Tr>
                    <Table.Th scope="col" style={{ padding: '12px', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Miesiąc (RRRR-MM)</Table.Th>
                    <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Liczba zgłoszonych potrzeb</Table.Th>
                    <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Wskaźnik dynamiki</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {series.map((point) => (
                    <Table.Tr key={point.month}>
                      <Table.Th scope="row" style={{ fontWeight: 600, padding: '12px', color: 'inherit' }}>
                        {point.month}
                      </Table.Th>
                      <Table.Td style={{ textAlign: 'right', padding: '12px', fontWeight: 700 }}>
                        {point.count}
                      </Table.Td>
                      <Table.Td style={{ textAlign: 'right', padding: '12px' }}>
                        {Math.round((point.count / maxSeriesCount) * 100)}% szczytu
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </div>
          </Stack>
        </Card>
      )}

      {/* Section 3: Municipalities and Geographic Distribution */}
      <Card withBorder padding="xl" radius="md">
        <Stack gap="lg">
          <div>
            <Group justify="space-between" align="baseline">
              <Title order={3} size="h3" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                3. Rozkład terytorialny zgłoszeń w gminach Małopolski
              </Title>
              <Badge color="indigo" variant="outline" size="lg">
                {byMunicipality.length} jednostek JST
              </Badge>
            </Group>
            <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem', marginTop: 4 }}>
              Statystyki z uwzględnieniem progu prywatności (gminy z liczbą zgłoszeń poniżej {privacyThreshold} są zagregowane).
            </Text>
          </div>

          {/* Visual Municipality Bars */}
          {viewMode === 'both' && (
            <Stack gap="sm" role="region" aria-label="Wizualizacja zgłoszeń w gminach">
              {sortedMunicipalities.map((item) => {
                const percentage = Math.round((item.count / maxMunicipalityCount) * 100)
                return (
                  <div key={item.municipality}>
                    <Group justify="space-between" mb={2}>
                      <Group gap="xs">
                        <IconMapPin size={16} color="var(--mantine-color-indigo-6)" aria-hidden="true" />
                        <Text fw={600} size="sm" style={{ fontSize: '0.95rem' }}>
                          {item.municipality}
                        </Text>
                      </Group>
                      <Text fw={700} size="sm" style={{ fontSize: '0.95rem' }}>
                        {item.count} {item.count === 1 ? 'potrzeba' : 'potrzeb'}
                      </Text>
                    </Group>
                    <Progress
                      value={percentage}
                      color="indigo"
                      size="md"
                      radius="xl"
                      aria-label={`Gmina ${item.municipality}: ${item.count} zgłoszonych potrzeb`}
                    />
                  </div>
                )
              })}
            </Stack>
          )}

          {/* Municipality Table */}
          <div style={{ overflowX: 'auto' }}>
            <Table
              striped
              highlightOnHover
              withTableBorder
              withColumnBorders
              aria-label="Tabela rozkładu potrzeb w gminach"
            >
              <caption style={{ textAlign: 'left', padding: '8px 0', fontWeight: 600, color: 'light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-2))' }}>
                Tabela 3: Zgłoszone potrzeby w podziale na gminy i powiaty województwa
              </caption>
              <Table.Thead style={{ backgroundColor: 'light-dark(var(--mantine-color-gray-1), var(--mantine-color-dark-6))' }}>
                <Table.Tr>
                  <Table.Th scope="col" style={{ padding: '12px', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Gmina / Miejscowość</Table.Th>
                  <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Liczba zgłoszeń</Table.Th>
                  <Table.Th scope="col" style={{ padding: '12px', textAlign: 'right', color: 'light-dark(var(--mantine-color-dark-8), var(--mantine-color-gray-1))' }}>Status prywatności</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {sortedMunicipalities.map((item) => (
                  <Table.Tr key={item.municipality}>
                    <Table.Th scope="row" style={{ fontWeight: 600, padding: '12px', color: 'inherit' }}>
                      {item.municipality}
                    </Table.Th>
                    <Table.Td style={{ textAlign: 'right', padding: '12px', fontWeight: 700 }}>
                      {item.count}
                    </Table.Td>
                    <Table.Td style={{ textAlign: 'right', padding: '12px' }}>
                      <Badge color="green" size="sm" variant="light">
                        Powyżej progu (≥{privacyThreshold})
                      </Badge>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </div>
        </Stack>
      </Card>

      {/* Section 4: Unmatched Queries / Knowledge Gaps */}
      <Card withBorder padding="xl" radius="md">
        <Stack gap="lg">
          <div>
            <Group justify="space-between" align="baseline">
              <Title order={3} size="h3" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                4. Czego szukają mieszkańcy? Luki w bibliotece innowacji
              </Title>
              <Badge color="orange" variant="outline" size="lg">
                {topUnmatchedTerms.length} kluczowych fraz
              </Badge>
            </Group>
            <Text size="sm" c="dimmed" style={{ fontSize: '0.95rem', marginTop: 4 }}>
              Najczęstsze zapytania mieszkańców w module Matchmakingu, dla których system nie znalazł
              satysfakcjonującego rozwiązania. To kluczowa wskazówka dla ROPS przy planowaniu kolejnych naborów grantowych.
            </Text>
          </div>

          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            {topUnmatchedTerms.map((term, index) => (
              <Card key={term} withBorder padding="md" radius="sm">
                <Group justify="space-between" align="center" wrap="nowrap">
                  <Group gap="sm" wrap="nowrap">
                    <ThemeIcon size={32} radius="xl" color="orange" variant="light">
                      <IconSearch size={16} aria-hidden="true" />
                    </ThemeIcon>
                    <div>
                      <Text fw={700} size="sm" style={{ fontSize: '1rem' }}>
                        „{term}”
                      </Text>
                      <Text size="xs" c="dimmed">
                        Pozycja #{index + 1} w niezaspokojonych zapytaniach
                      </Text>
                    </div>
                  </Group>

                  {onNavigateTab && (
                    <Button
                      variant="subtle"
                      color="blue"
                      size="xs"
                      rightSection={<IconPlus size={14} aria-hidden="true" />}
                      onClick={() => onNavigateTab('tresci', 'innowacje')}
                      aria-label={`Dodaj nową innowację odpowiadającą na potrzebę ${term}`}
                    >
                      Dodaj innowację
                    </Button>
                  )}
                </Group>
              </Card>
            ))}
          </SimpleGrid>

          <Alert
            color="yellow"
            icon={<IconAlertCircle size={22} aria-hidden="true" />}
            radius="md"
            title="Rekomendacja dla zespołu koordynatorów ROPS"
          >
            Frazy z tej listy warto uwzględnić w kryteriach premiowanych w najbliższym naborze grantowym
            jako priorytetowe wyzwania społeczne dla innowatorów.
          </Alert>
        </Stack>
      </Card>
    </Stack>
  )
}
