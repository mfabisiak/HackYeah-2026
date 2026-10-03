import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Card,
  Group,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
  Anchor,
} from '@mantine/core'
import {
  IconArrowRight,
  IconCheck,
  IconFilterOff,
  IconSearch,
} from '@tabler/icons-react'
import { useEffect, useState, useTransition } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { EmptyState } from '../../components/EmptyState'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { AccessiblePagination } from '../../components/Pagination'
import {
  INNOVATION_STAGE_NAMES,
  SOCIAL_AREA_NAMES,
  SOCIAL_AREA_OPTIONS,
  TARGET_GROUP_NAMES,
  TARGET_GROUP_OPTIONS,
} from './constants'
import type { InnovationSummaryJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function InnovationsListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [, startTransition] = useTransition()

  const qParam = searchParams.get('q') ?? ''
  const areaParam = searchParams.get('area') ?? ''
  const targetGroupParam = searchParams.get('targetGroup') ?? ''
  const pageParam = parseInt(searchParams.get('page') ?? '1', 10)
  const currentPage = isNaN(pageParam) || pageParam < 1 ? 1 : pageParam

  const [searchInput, setSearchInput] = useState(qParam)
  const [data, setData] = useState<PageJs<InnovationSummaryJs> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  const [prevQParam, setPrevQParam] = useState(qParam)
  if (prevQParam !== qParam) {
    setPrevQParam(qParam)
    setSearchInput(qParam)
  }

  useEffect(() => {
    document.title = 'Baza innowacji społecznych | HubMI'
  }, [])

  useEffect(() => {
    let cancelled = false

    const fetchInnovations = async () => {
      try {
        const res = await hubApi.innovations.list(
          qParam.trim() || undefined,
          areaParam || undefined,
          targetGroupParam || undefined,
          currentPage - 1,
          PAGE_SIZE,
        )

        if (cancelled) return

        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać listy innowacji.')
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd sieci podczas pobierania danych. Spróbuj ponownie później.')
          setData(null)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchInnovations()

    return () => {
      cancelled = true
    }
  }, [qParam, areaParam, targetGroupParam, currentPage, refreshTrigger])

  const updateFilters = (newParams: Record<string, string | null>) => {
    let hasChanged = false
    const next = new URLSearchParams(searchParams)

    Object.entries(newParams).forEach(([key, val]) => {
      const prevVal = searchParams.get(key)
      const isDefaultPage = key === 'page' && (!val || val === '1')
      const trimmedVal = val && val.trim() !== '' && !isDefaultPage ? val.trim() : null

      if (trimmedVal !== null) {
        if (prevVal !== trimmedVal) hasChanged = true
        next.set(key, trimmedVal)
      } else {
        if (prevVal !== null) hasChanged = true
        next.delete(key)
      }
    })

    setIsLoading(true)
    setError(null)

    if (hasChanged) {
      startTransition(() => {
        setSearchParams(next)
      })
    } else {
      setRefreshTrigger((c) => c + 1)
    }
  }

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    updateFilters({ q: searchInput, page: '1' })
  }

  const handleAreaChange = (val: string | null) => {
    updateFilters({ area: val, page: '1' })
  }

  const handleTargetGroupChange = (val: string | null) => {
    updateFilters({ targetGroup: val, page: '1' })
  }

  const handleResetFilters = () => {
    setSearchInput('')
    updateFilters({ q: null, area: null, targetGroup: null, page: '1' })
  }

  const handlePageChange = (newPage: number) => {
    updateFilters({ page: newPage.toString() })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const hasActiveFilters = Boolean(qParam || areaParam || targetGroupParam)
  const totalPages = data ? Math.ceil(data.total / PAGE_SIZE) : 0

  return (
    <Stack gap="xl">
      <PageHeader
        title="Baza innowacji społecznych"
        subtitle="Przeglądaj sprawdzone i przetestowane rozwiązania dla mieszkańców Małopolski."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Baza innowacji' },
        ]}
      />

      {/* Screen reader announcer for search results */}
      <Box
        role="status"
        aria-live="polite"
        style={{
          position: 'absolute',
          width: 1,
          height: 1,
          margin: -1,
          padding: 0,
          overflow: 'hidden',
          clip: 'rect(0, 0, 0, 0)',
          border: 0,
        }}
      >
        {!isLoading && data && (
          `Znaleziono ${data.total} innowacji społecznych.`
        )}
      </Box>

      {/* Search & Filters Section */}
      <Card withBorder padding="lg" radius="md">
        <form onSubmit={handleSearchSubmit}>
          <Stack gap="md">
            <TextInput
              id="search-input"
              label="Wyszukaj innowację po nazwie lub opisie"
              placeholder="Wpisz słowo, np. transport seniorów, pomoc sąsiedzka, asystent..."
              size="lg"
              radius="md"
              value={searchInput}
              onChange={(e) => setSearchInput(e.currentTarget.value)}
              leftSection={
                <ActionIcon
                  type="submit"
                  variant="subtle"
                  color="gray"
                  size="lg"
                  aria-label="Szukaj w bazie innowacji"
                >
                  <IconSearch size={22} aria-hidden="true" />
                </ActionIcon>
              }
              leftSectionPointerEvents="all"
              rightSection={
                <Button type="submit" size="sm" radius="md">
                  Szukaj
                </Button>
              }
              rightSectionWidth={85}
              styles={{
                label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 6 },
                input: { fontSize: '1rem' },
              }}
            />

            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              <Select
                id="area-filter"
                label="Obszar tematyczny"
                placeholder="Wszystkie obszary"
                data={SOCIAL_AREA_OPTIONS}
                value={areaParam || null}
                onChange={handleAreaChange}
                clearable
                searchable
                size="md"
                radius="md"
                styles={{
                  label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 6 },
                  input: { fontSize: '1.05rem' },
                }}
              />
              <Select
                id="target-group-filter"
                label="Dla kogo (grupa docelowa)"
                placeholder="Wszystkie grupy"
                data={TARGET_GROUP_OPTIONS}
                value={targetGroupParam || null}
                onChange={handleTargetGroupChange}
                clearable
                searchable
                size="md"
                radius="md"
                styles={{
                  label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 6 },
                  input: { fontSize: '1.05rem' },
                }}
              />
            </SimpleGrid>

            {hasActiveFilters && (
              <Group justify="flex-end">
                <Button
                  variant="subtle"
                  color="gray"
                  size="sm"
                  leftSection={<IconFilterOff size={16} aria-hidden="true" />}
                  onClick={handleResetFilters}
                >
                  Wyczyść wszystkie filtry
                </Button>
              </Group>
            )}
          </Stack>
        </form>
      </Card>

      {/* Main Content State */}
      {isLoading && (
        <LoadingState message="Wczytuję innowacje społeczne z bazy..." minHeight={200} />
      )}

      {error && (
        <ErrorAlert
          title="Błąd ładowania innowacji"
          message={error}
          onRetry={() => updateFilters({})}
        />
      )}

      {!isLoading && !error && data && data.items.length === 0 && (
        <EmptyState
          title="Brak innowacji spełniających podane kryteria"
          description={
            hasActiveFilters
              ? 'Nie znaleźliśmy innowacji pasujących do wybranych filtrów. Spróbuj wpisać inne słowo kluczowe lub zresetować filtry.'
              : 'W bazie nie ma jeszcze zarejestrowanych innowacji społecznych.'
          }
          action={
            hasActiveFilters ? (
              <Button size="md" onClick={handleResetFilters}>
                Pokaż wszystkie innowacje
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && data && data.items.length > 0 && (
        <Stack gap="lg">
          <Group justify="space-between" align="center">
            <Text
              size="md"
              fw={600}
              c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
            >
              Znaleziono {data.total} {data.total === 1 ? 'rozwiązanie' : 'rozwiązań'}:
            </Text>
          </Group>

          <Stack component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }}>
            {data.items.map((item) => (
              <li key={item.id}>
                <Card withBorder padding="lg" radius="md">
                  <Stack gap="md">
                    <Group justify="space-between" align="center" wrap="wrap" gap="sm">
                      <Badge
                        variant="outline"
                        color="gray"
                        size="xl"
                        radius="md"
                        leftSection={<IconCheck size={18} aria-hidden="true" />}
                        style={{
                          height: 38,
                          borderWidth: 2,
                          borderColor: 'var(--mantine-color-gray-6)',
                          display: 'inline-flex',
                          alignItems: 'center',
                        }}
                        styles={{
                          label: { fontSize: '0.95rem', fontWeight: 600 },
                        }}
                      >
                        {INNOVATION_STAGE_NAMES[item.stage] ?? item.stage}
                      </Badge>
                    </Group>

                    <Title order={2} size="h3" style={{ lineHeight: 1.3 }}>
                      <Anchor
                        component={Link}
                        to={`/innowacje/${item.id}`}
                        underline="hover"
                        c="inherit"
                        style={{ fontSize: '1.35rem', fontWeight: 700 }}
                      >
                        {item.title}
                      </Anchor>
                    </Title>

                    <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                      {item.summary}
                    </Text>

                    {item.areas && item.areas.length > 0 && (
                      <Group gap="sm" wrap="wrap" mt="xs">
                        {item.areas.map((area) => (
                          <Badge
                            key={area}
                            variant="light"
                            color="blue"
                            size="xl"
                            radius="md"
                            style={{
                              height: 38,
                              border: '1.5px solid var(--mantine-color-blue-light-color)',
                              display: 'inline-flex',
                              alignItems: 'center',
                            }}
                            styles={{
                              label: { fontSize: '1.05rem', fontWeight: 600 },
                            }}
                          >
                            {SOCIAL_AREA_NAMES[area] ?? area}
                          </Badge>
                        ))}
                      </Group>
                    )}

                    {item.targetGroups && item.targetGroups.length > 0 && (
                      <Group gap="sm" wrap="wrap" align="center" mt="xs">
                        <Text
                          style={{
                            fontSize: '1.15rem',
                            fontWeight: 700,
                          }}
                          c="light-dark(var(--mantine-color-gray-8), var(--mantine-color-dark-0))"
                        >
                          Dla kogo:
                        </Text>
                        {item.targetGroups.map((tg) => (
                          <Badge
                            key={tg}
                            variant="light"
                            color="teal"
                            size="xl"
                            radius="md"
                            style={{
                              height: 38,
                              border: '1.5px solid var(--mantine-color-teal-light-color)',
                              display: 'inline-flex',
                              alignItems: 'center',
                            }}
                            styles={{
                              label: { fontSize: '1.05rem', fontWeight: 600 },
                            }}
                          >
                            {TARGET_GROUP_NAMES[tg] ?? tg}
                          </Badge>
                        ))}
                      </Group>
                    )}

                    <Group justify="flex-start" mt="sm">
                      <Button
                        component={Link}
                        to={`/innowacje/${item.id}`}
                        variant="light"
                        size="md"
                        rightSection={<IconArrowRight size={18} aria-hidden="true" />}
                        styles={{
                          root: { fontSize: '1rem', fontWeight: 600 },
                        }}
                      >
                        Zobacz szczegóły rozwiązania
                      </Button>
                    </Group>
                  </Stack>
                </Card>
              </li>
            ))}
          </Stack>

          <AccessiblePagination
            total={totalPages}
            value={currentPage}
            onChange={handlePageChange}
            totalCount={data.total}
            itemsPerPage={PAGE_SIZE}
            ariaLabel="Paginacja bazy innowacji"
          />
        </Stack>
      )}
    </Stack>
  )
}
