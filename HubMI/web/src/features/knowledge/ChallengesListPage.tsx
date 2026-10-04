import {
  Badge,
  Box,
  Button,
  Card,
  Group,
  Select,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import {
  IconArrowRight,
  IconFilterOff,
  IconMapPin,
} from '@tabler/icons-react'
import { useEffect, useState, useTransition } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { toFriendlyErrorMessage } from '../../api/errors'
import { hubApi } from '../../api/hubApi'
import { EmptyState } from '../../components/EmptyState'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { AccessiblePagination } from '../../components/Pagination'
import { MALOPOLSKA_MUNICIPALITIES } from '../matchmaking/constants'
import { SOCIAL_AREA_NAMES, SOCIAL_AREA_OPTIONS } from './constants'
import type { ChallengeJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function ChallengesListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [, startTransition] = useTransition()

  const areaParam = searchParams.get('area') ?? ''
  const municipalityParam = searchParams.get('municipality') ?? ''
  const pageParam = parseInt(searchParams.get('page') ?? '1', 10)
  const currentPage = isNaN(pageParam) || pageParam < 1 ? 1 : pageParam

  const [data, setData] = useState<PageJs<ChallengeJs> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  useEffect(() => {
    document.title = 'Katalog wyzwań Małopolski | HubMI'
  }, [])

  useEffect(() => {
    let cancelled = false

    const fetchChallenges = async () => {
      try {
        const res = await hubApi.challenges.list(
          areaParam || undefined,
          currentPage - 1,
          PAGE_SIZE,
        )

        if (cancelled) return

        if (res.error) {
          setError(toFriendlyErrorMessage(res.error, 'Nie udało się pobrać listy wyzwań.'))
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyErrorMessage(err, 'Wystąpił błąd podczas ładowania wyzwań.'))
          setData(null)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchChallenges()

    return () => {
      cancelled = true
    }
  }, [areaParam, currentPage, refreshTrigger])

  const updateFilters = (newParams: Record<string, string | null>) => {
    let hasChanged = false
    let areaOrPageChanged = false
    const next = new URLSearchParams(searchParams)

    Object.entries(newParams).forEach(([key, val]) => {
      const prevVal = searchParams.get(key)
      const isDefaultPage = key === 'page' && (!val || val === '1')
      const trimmedVal = val && val.trim() !== '' && !isDefaultPage ? val.trim() : null

      if (trimmedVal !== null) {
        if (prevVal !== trimmedVal) {
          hasChanged = true
          if (key === 'area' || key === 'page') areaOrPageChanged = true
        }
        next.set(key, trimmedVal)
      } else {
        if (prevVal !== null) {
          hasChanged = true
          if (key === 'area' || key === 'page') areaOrPageChanged = true
        }
        next.delete(key)
      }
    })

    if (areaOrPageChanged || !data) {
      setIsLoading(true)
      setError(null)
    }

    if (hasChanged) {
      startTransition(() => {
        setSearchParams(next)
      })
    } else if (areaOrPageChanged || !data) {
      setRefreshTrigger((c) => c + 1)
    }
  }

  const handleAreaChange = (val: string | null) => {
    updateFilters({ area: val, page: '1' })
  }

  const handleMunicipalityChange = (val: string | null) => {
    updateFilters({ municipality: val, page: '1' })
  }

  const handleResetFilters = () => {
    updateFilters({ area: null, municipality: null, page: '1' })
  }

  const handlePageChange = (newPage: number) => {
    updateFilters({ page: newPage.toString() })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Filter items in memory if municipality filter is active
  const filteredItems = data?.items
    ? data.items.filter((item) => {
        if (!municipalityParam) return true
        return item.municipalities.some(
          (m) => m.toLowerCase() === municipalityParam.toLowerCase(),
        )
      })
    : []

  const hasActiveFilters = Boolean(areaParam || municipalityParam)
  const totalPages = data ? Math.ceil(data.total / PAGE_SIZE) : 0

  return (
    <Stack gap="xl">
      <PageHeader
        title="Katalog wyzwań społecznych Małopolski"
        subtitle="Zidentyfikowane potrzeby i problemy lokalne, przed którymi stoją małopolskie gminy i mieszkańcy."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Wyzwania Małopolski' },
        ]}
      />

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
          `Znaleziono ${filteredItems.length} wyzwań społecznych.`
        )}
      </Box>

      {/* Filter Card */}
      <Card withBorder padding="lg" radius="md">
        <Stack gap="md">
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            <Select
              id="area-filter"
              label="Filtruj wg obszaru problemu"
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
              id="municipality-filter"
              label="Filtruj wg gminy w Małopolsce"
              placeholder="Wszystkie gminy"
              data={MALOPOLSKA_MUNICIPALITIES.map((name) => ({ value: name, label: name }))}
              value={municipalityParam || null}
              onChange={handleMunicipalityChange}
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
                Wyczyść filtry
              </Button>
            </Group>
          )}
        </Stack>
      </Card>

      {/* Main Content */}
      {isLoading && (
        <LoadingState message="Ładuję wyzwania społeczne..." minHeight={200} />
      )}

      {error && (
        <ErrorAlert
          title="Błąd ładowania wyzwań"
          message={error}
          onRetry={() => updateFilters({})}
        />
      )}

      {!isLoading && !error && filteredItems.length === 0 && (
        <EmptyState
          title="Brak wyzwań dla wybranych filtrów"
          description={
            hasActiveFilters
              ? 'W wybranym obszarze lub gminie nie ma jeszcze zarejestrowanych wyzwań. Zmień kryteria filtrowania.'
              : 'W bazie nie ma jeszcze zapisanych wyzwań regionalnych.'
          }
          action={
            hasActiveFilters ? (
              <Button size="md" onClick={handleResetFilters}>
                Wyczyść filtry
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && filteredItems.length > 0 && (
        <Stack gap="lg">
          <Group justify="space-between" align="center">
            <Text
              size="md"
              fw={600}
              c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
            >
              Znaleziono {filteredItems.length}{' '}
              {filteredItems.length === 1 ? 'wyzwanie' : 'wyzwań'}:
            </Text>
          </Group>

          <Stack component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }}>
            {filteredItems.map((challenge) => (
              <li key={challenge.id}>
                <Card withBorder padding="lg" radius="md">
                  <Stack gap="md">
                    <Group justify="space-between" align="center" wrap="wrap" gap="sm">
                      <Badge
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
                        {SOCIAL_AREA_NAMES[challenge.area] ?? challenge.area}
                      </Badge>
                    </Group>

                    <Title order={2} size="h3" style={{ lineHeight: 1.3 }}>
                      {challenge.title}
                    </Title>

                    <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                      {challenge.description}
                    </Text>

                    {challenge.municipalities && challenge.municipalities.length > 0 && (
                      <Stack gap="xs" mt="xs">
                        <Text
                          style={{
                            fontSize: '1.15rem',
                            fontWeight: 700,
                          }}
                          c="light-dark(var(--mantine-color-gray-8), var(--mantine-color-dark-0))"
                        >
                          Zgłoszone z gmin:
                        </Text>
                        <Group gap="sm" wrap="wrap">
                          {challenge.municipalities.map((muni) => (
                            <Badge
                              key={muni}
                              variant="outline"
                              color="gray"
                              size="xl"
                              radius="md"
                              leftSection={<IconMapPin size={18} aria-hidden="true" />}
                              style={{
                                height: 38,
                                borderWidth: 2,
                                borderColor: 'light-dark(var(--mantine-color-gray-5), var(--mantine-color-dark-4))',
                                display: 'inline-flex',
                                alignItems: 'center',
                                backgroundColor: 'light-dark(var(--mantine-color-gray-1), var(--mantine-color-dark-6))',
                              }}
                              styles={{
                                label: {
                                  fontSize: '1.05rem',
                                  fontWeight: 600,
                                  color: 'light-dark(var(--mantine-color-gray-9), var(--mantine-color-dark-0))',
                                },
                              }}
                            >
                              {muni}
                            </Badge>
                          ))}
                        </Group>
                      </Stack>
                    )}

                    <Group justify="flex-start" mt="sm">
                      <Button
                        component={Link}
                        to={`/innowacje?area=${challenge.area}`}
                        variant="light"
                        size="md"
                        rightSection={<IconArrowRight size={18} aria-hidden="true" />}
                        styles={{
                          root: { fontSize: '1rem', fontWeight: 600 },
                        }}
                      >
                        Zobacz pasujące innowacje dla tego wyzwania
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
            totalCount={data?.total}
            itemsPerPage={PAGE_SIZE}
            ariaLabel="Paginacja wyzwań"
          />
        </Stack>
      )}
    </Stack>
  )
}
