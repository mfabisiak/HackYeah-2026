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
  TextInput,
  Title,
} from '@mantine/core'
import {
  IconBook,
  IconDownload,
  IconFileAnalytics,
  IconFileText,
  IconFilterOff,
  IconPlayerPlay,
  IconSearch,
  IconTemplate,
} from '@tabler/icons-react'
import { useEffect, useState, useTransition } from 'react'
import { useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { EmptyState } from '../../components/EmptyState'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { AccessiblePagination } from '../../components/Pagination'
import {
  MATERIAL_TYPE_NAMES,
  MATERIAL_TYPE_OPTIONS,
  SOCIAL_AREA_NAMES,
  SOCIAL_AREA_OPTIONS,
} from './constants'
import type { MaterialJs, PageJs } from 'hubmi-client'

const PAGE_SIZE = 10

export function MaterialsListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [, startTransition] = useTransition()

  const qParam = searchParams.get('q') ?? ''
  const typeParam = searchParams.get('type') ?? ''
  const areaParam = searchParams.get('area') ?? ''
  const pageParam = parseInt(searchParams.get('page') ?? '1', 10)
  const currentPage = isNaN(pageParam) || pageParam < 1 ? 1 : pageParam

  const [searchInput, setSearchInput] = useState(qParam)
  const [data, setData] = useState<PageJs<MaterialJs> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [prevQParam, setPrevQParam] = useState(qParam)
  if (prevQParam !== qParam) {
    setPrevQParam(qParam)
    setSearchInput(qParam)
  }

  useEffect(() => {
    document.title = 'Materiały i publikacje edukacyjne | HubMI'
  }, [])

  useEffect(() => {
    let cancelled = false

    const fetchMaterials = async () => {
      try {
        const res = await hubApi.materials.list(
          qParam.trim() || undefined,
          areaParam || undefined,
          typeParam || undefined,
          currentPage - 1,
          PAGE_SIZE,
        )

        if (cancelled) return

        if (res.error) {
          setError(res.error.message || 'Nie udało się pobrać materiałów edukacyjnych.')
          setData(null)
        } else if (res.value) {
          setData(res.value)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd sieci podczas ładowania materiałów.')
          setData(null)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchMaterials()

    return () => {
      cancelled = true
    }
  }, [qParam, areaParam, typeParam, currentPage])

  const updateFilters = (newParams: Record<string, string | null>) => {
    setIsLoading(true)
    setError(null)
    startTransition(() => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        Object.entries(newParams).forEach(([key, val]) => {
          if (val && val.trim() !== '') {
            next.set(key, val)
          } else {
            next.delete(key)
          }
        })
        return next
      })
    })
  }

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    updateFilters({ q: searchInput, page: '1' })
  }

  const handleTypeChange = (val: string | null) => {
    updateFilters({ type: val, page: '1' })
  }

  const handleAreaChange = (val: string | null) => {
    updateFilters({ area: val, page: '1' })
  }

  const handleResetFilters = () => {
    setSearchInput('')
    updateFilters({ q: null, type: null, area: null, page: '1' })
  }

  const handlePageChange = (newPage: number) => {
    updateFilters({ page: newPage.toString() })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'GUIDE':
        return <IconBook size={20} aria-hidden="true" />
      case 'REPORT':
        return <IconFileAnalytics size={20} aria-hidden="true" />
      case 'VIDEO':
        return <IconPlayerPlay size={20} aria-hidden="true" />
      case 'CANVAS':
        return <IconTemplate size={20} aria-hidden="true" />
      default:
        return <IconFileText size={20} aria-hidden="true" />
    }
  }

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'GUIDE':
        return 'blue'
      case 'REPORT':
        return 'teal'
      case 'VIDEO':
        return 'red'
      case 'CANVAS':
        return 'grape'
      default:
        return 'gray'
    }
  }

  const hasActiveFilters = Boolean(qParam || typeParam || areaParam)
  const totalPages = data ? Math.ceil(data.total / PAGE_SIZE) : 0

  return (
    <Stack gap="xl">
      <PageHeader
        title="Materiały i publikacje edukacyjne"
        subtitle="Poradniki krok po kroku, raporty z diagnoz, szablony robocze oraz instruktaże wideo wspierające innowacje."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Materiały edukacyjne' },
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
          `Znaleziono ${data.total} materiałów edukacyjnych.`
        )}
      </Box>

      {/* Filter Card */}
      <Card withBorder padding="lg" radius="md">
        <form onSubmit={handleSearchSubmit}>
          <Stack gap="md">
            <TextInput
              id="material-search"
              label="Wyszukaj materiał edukacyjny"
              placeholder="Wpisz słowo kluczowe, np. poradnik, canvas, diagnoza, dostępność..."
              size="lg"
              radius="md"
              value={searchInput}
              onChange={(e) => setSearchInput(e.currentTarget.value)}
              leftSection={<IconSearch size={20} aria-hidden="true" />}
              rightSection={
                <Button type="submit" size="sm" radius="md">
                  Szukaj
                </Button>
              }
              rightSectionWidth={85}
              styles={{
                label: { fontSize: '1.05rem', fontWeight: 600, marginBottom: 6 },
              }}
            />

            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              <Select
                id="material-type-filter"
                label="Typ materiału"
                placeholder="Wszystkie typy"
                data={MATERIAL_TYPE_OPTIONS}
                value={typeParam || null}
                onChange={handleTypeChange}
                clearable
                searchable
                size="md"
                radius="md"
                styles={{
                  label: { fontSize: '0.95rem', fontWeight: 600, marginBottom: 4 },
                }}
              />
              <Select
                id="material-area-filter"
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
                  label: { fontSize: '0.95rem', fontWeight: 600, marginBottom: 4 },
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
        </form>
      </Card>

      {/* Main Content */}
      {isLoading && (
        <LoadingState message="Wczytuję materiały edukacyjne..." minHeight={200} />
      )}

      {error && (
        <ErrorAlert
          title="Błąd ładowania materiałów"
          message={error}
          onRetry={() => updateFilters({})}
        />
      )}

      {!isLoading && !error && data && data.items.length === 0 && (
        <EmptyState
          title="Brak materiałów spełniających podane kryteria"
          description={
            hasActiveFilters
              ? 'Nie znaleziono materiałów pasujących do podanych filtrów. Spróbuj zmienić słowo kluczowe lub zresetować filtry.'
              : 'W bazie nie ma jeszcze opublikowanych materiałów.'
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

      {!isLoading && !error && data && data.items.length > 0 && (
        <Stack gap="lg">
          <Group justify="space-between" align="center">
            <Text
              size="md"
              fw={600}
              c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
            >
              Znaleziono {data.total} {data.total === 1 ? 'materiał' : 'materiałów'}:
            </Text>
          </Group>

          <Stack component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }}>
            {data.items.map((item) => (
              <li key={item.id}>
                <Card withBorder padding="lg" radius="md">
                  <Stack gap="md">
                    <Group justify="space-between" align="center" wrap="wrap" gap="sm">
                      <Badge
                        variant="light"
                        color={getTypeColor(item.type)}
                        size="xl"
                        radius="md"
                        leftSection={getTypeIcon(item.type)}
                        style={{
                          height: 38,
                          display: 'inline-flex',
                          alignItems: 'center',
                        }}
                        styles={{
                          label: { fontSize: '0.95rem', fontWeight: 600 },
                        }}
                      >
                        {MATERIAL_TYPE_NAMES[item.type] ?? item.type}
                      </Badge>
                    </Group>

                    <Title order={2} size="h3" style={{ lineHeight: 1.3 }}>
                      {item.title}
                    </Title>

                    <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
                      {item.description}
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

                    <Group justify="flex-start" mt="sm">
                      <Button
                        component="a"
                        href={item.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        variant="filled"
                        size="md"
                        leftSection={
                          item.type === 'VIDEO' ? (
                            <IconPlayerPlay size={20} aria-hidden="true" />
                          ) : (
                            <IconDownload size={20} aria-hidden="true" />
                          )
                        }
                        styles={{
                          root: { fontSize: '1rem', fontWeight: 600 },
                        }}
                      >
                        {item.type === 'VIDEO'
                          ? 'Obejrzyj materiał wideo (z napisami)'
                          : 'Pobierz materiał (PDF)'}
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
            ariaLabel="Paginacja materiałów"
          />
        </Stack>
      )}
    </Stack>
  )
}
