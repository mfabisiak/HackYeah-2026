import { useRef, useState } from 'react'
import {
  Box,
  Button,
  Container,
  Group,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { IconArrowRight, IconBulb, IconSparkles } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { EmptyState } from '../../components/EmptyState'
import { ErrorAlert } from '../../components/ErrorAlert'
import { LoadingState } from '../../components/LoadingState'
import { MatchCard } from './MatchCard'
import { MatchFeedback } from './MatchFeedback'
import { MatchmakingForm } from './MatchmakingForm'
import { SimilarNeedsList } from './SimilarNeedsList'
import type { MatchmakingResultData } from './types'

export function MatchmakingView() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<MatchmakingResultData | null>(null)
  const [lastQuery, setLastQuery] = useState<{
    description: string
    municipality?: string
  } | null>(null)

  const resultsRef = useRef<HTMLHeadingElement>(null)

  const handleSearch = async (description: string, municipality?: string) => {
    setIsLoading(true)
    setError(null)
    setLastQuery({ description, municipality })

    try {
      const res = await hubApi.matches.match(description, municipality)
      if (!res.ok || !res.value) {
        if (res.error?.status === 429) {
          throw new Error(
            'Zbyt wiele zapytań w krótkim czasie. Odczekaj chwilę przed kolejnym wyszukiwaniem.',
          )
        }
        throw new Error(
          res.error?.message ?? 'Nie udało się dopasować rozwiązań. Spróbuj ponownie.',
        )
      }

      const val = res.value
      const mappedData: MatchmakingResultData = {
        needId: val.needId,
        noGoodMatch: val.noGoodMatch,
        matches: Array.from(val.matches).map((m) => ({
          score: m.score,
          reasons: Array.from(m.reasons),
          matchedTerms: Array.from(m.matchedTerms),
          innovation: {
            id: m.innovation.id,
            title: m.innovation.title,
            summary: m.innovation.summary,
            areas: Array.from(m.innovation.areas),
            targetGroups: Array.from(m.innovation.targetGroups),
            stage: m.innovation.stage,
          },
        })),
        similarNeeds: Array.from(val.similarNeeds).map((s) => ({
          id: s.id,
          excerpt: s.excerpt,
          area: s.area,
        })),
      }

      setResult(mappedData)

      // Move focus to results heading so screen reader announces results immediately
      setTimeout(() => {
        resultsRef.current?.focus()
      }, 50)
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Wystąpił nieoczekiwany błąd podczas wyszukiwania rozwiązań.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const handleRetry = () => {
    if (lastQuery) {
      void handleSearch(lastQuery.description, lastQuery.municipality)
    }
  }

  return (
    <Container size="md" p={0}>
      <Stack gap="xl">
        <Box component="header">
          <Stack gap="xs">
            <Group gap="xs">
              <IconSparkles size={24} color="var(--mantine-color-blue-filled)" aria-hidden="true" />
              <Title order={1} size="h2">
                Opisz problem, znajdź rozwiązanie
              </Title>
            </Group>
            <Text size="md" c="dimmed">
              Nasz inteligentny asystent przeszuka bazę sprawdzonych innowacji społecznych w Małopolsce
              i wskaże rozwiązania najbardziej odpowiadające Twojej sytuacji wraz z uzasadnieniem.
            </Text>
          </Stack>
        </Box>

        <MatchmakingForm isLoading={isLoading} onSubmit={handleSearch} />

        {isLoading && (
          <LoadingState
            message="Szukam najlepszych rozwiązań dla Twojego problemu w bazie innowacji..."
            minHeight={160}
          />
        )}

        {error && (
          <ErrorAlert
            title="Błąd wyszukiwania"
            message={error}
            onRetry={lastQuery ? handleRetry : undefined}
          />
        )}

        {result && !isLoading && (
          <Stack gap="lg" mt="md" component="section" aria-label="Wyniki dopasowania">
            <Title
              ref={resultsRef}
              tabIndex={-1}
              order={2}
              size="h3"
              style={{ outline: 'none' }}
              data-testid="results-heading"
            >
              {result.noGoodMatch || result.matches.length === 0
                ? 'Wyniki dopasowania'
                : `Znalezione rozwiązania (${result.matches.length})`}
            </Title>

            {result.noGoodMatch || result.matches.length === 0 ? (
              <EmptyState
                icon={<IconBulb size={32} />}
                title="Nie znaleźliśmy jeszcze idealnie dopasowanego rozwiązania"
                description="Twój problem może być nowym wyzwaniem społecznym, które nie ma jeszcze gotowego wzorca w bazie. Możesz spróbować doprecyzować opis lub zgłosić pomysł na nową innowację."
                action={
                  <Button
                    component={Link}
                    to="/pomysly/nowy"
                    variant="light"
                    rightSection={<IconArrowRight size={16} aria-hidden="true" />}
                  >
                    Zgłoś własny pomysł na innowację
                  </Button>
                }
              />
            ) : (
              <>
                <Stack
                  component="ul"
                  gap="md"
                  p={0}
                  m={0}
                  style={{ listStyle: 'none' }}
                  aria-label="Lista dopasowanych innowacji"
                >
                  {result.matches.map((item) => (
                    <li key={item.innovation.id}>
                      <MatchCard match={item} />
                    </li>
                  ))}
                </Stack>

                <MatchFeedback needId={result.needId} />

                {result.similarNeeds.length > 0 && (
                  <SimilarNeedsList items={result.similarNeeds} />
                )}
              </>
            )}
          </Stack>
        )}
      </Stack>
    </Container>
  )
}
