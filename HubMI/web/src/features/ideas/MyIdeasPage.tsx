import { useEffect, useMemo, useState } from 'react'
import { Anchor, Badge, Button, Card, Group, Stack, Text, Title, VisuallyHidden } from '@mantine/core'
import { IconBulb, IconPlus, IconRefreshAlert } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import type { IdeaJs } from 'hubmi-client'
import { Link } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { formatApiError } from '../../api/errors'
import { unwrapApiResult } from '../../api/queryClient'
import { AccessiblePagination, EmptyState, ErrorAlert, LoadingState, PageHeader } from '../../components'
import { formatDate } from '../../components/dates'
import { INNOVATION_STAGE_NAMES, TARGET_GROUP_NAMES } from './constants'
import { IdeaStatusBadge } from './IdeaStatusBadge'
import { loadSeenIdeas, saveSeenIdeas } from './localData'

const PAGE_SIZE = 10

/** Small non-cryptographic hash, so the admin's comment itself is never stored in the browser. */
const hash = (text: string): string =>
  [...text].reduce((acc, char) => (Math.imul(acc, 31) + char.charCodeAt(0)) | 0, 0).toString(36)

/** What the author last saw of an idea: its status and (a hash of) the admin's comment. */
export const ideaSignature = (idea: IdeaJs): string => `${idea.status}|${hash(idea.adminComment ?? '')}`

export function MyIdeasPage() {
  const [page, setPage] = useState(0)
  // Snapshot of the previous visit; the stored copy is refreshed below, after the changes have been computed.
  const [seen] = useState(loadSeenIdeas)
  const ideas = useQuery({
    queryKey: ['ideas', 'mine', page],
    queryFn: () => unwrapApiResult(hubApi.ideas.mine(page, PAGE_SIZE)),
    refetchOnMount: 'always',
  })
  const items = useMemo(() => ideas.data?.items ?? [], [ideas.data])

  useEffect(() => {
    document.title = 'Moje pomysły | HubMI'
  }, [])

  useEffect(() => {
    if (items.length === 0) return
    saveSeenIdeas({
      ...loadSeenIdeas(),
      ...Object.fromEntries(items.map((idea) => [idea.id, ideaSignature(idea)])),
    })
  }, [items])

  const total = ideas.data?.total ?? 0

  return (
    <Stack gap="lg">
      <PageHeader
        title="Moje pomysły"
        subtitle="Tu zobaczysz, co dzieje się z pomysłami, które zgłosiłeś(-aś) do ROPS."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje pomysły' }]}
        actions={
          <Button component={Link} to="/pomysly/nowy" leftSection={<IconPlus size={18} aria-hidden="true" />}>
            Zgłoś nowy pomysł
          </Button>
        }
      />

      {ideas.isPending && <LoadingState message="Wczytywanie Twoich pomysłów..." />}
      {ideas.isError && (
        <ErrorAlert title="Nie udało się wczytać pomysłów" message={formatApiError(ideas.error)} onRetry={() => void ideas.refetch()} />
      )}

      {ideas.isSuccess && (
        <>
          <Text role="status">{total === 0 ? 'Nie zgłoszono jeszcze żadnego pomysłu.' : `Liczba Twoich pomysłów: ${total}.`}</Text>
          {total === 0 ? (
            <EmptyState
              icon={<IconBulb size={32} />}
              title="Nie masz jeszcze żadnych pomysłów"
              description="Zgłoś pierwszy pomysł na innowację społeczną. Wystarczy krótka fiszka."
              action={
                <Button component={Link} to="/pomysly/nowy">
                  Zgłoś pierwszy pomysł
                </Button>
              }
            />
          ) : (
            <Stack component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }} aria-label="Lista Twoich pomysłów">
              {items.map((idea) => {
                const previous = seen[idea.id]
                const changed = previous !== undefined && previous !== ideaSignature(idea)
                return (
                  <li key={idea.id}>
                    <Card withBorder padding="lg" radius="md">
                      <Stack gap="sm">
                        <Group justify="space-between" align="flex-start" gap="sm">
                          <Title order={2} size="h4">
                            <Anchor component={Link} to={`/pomysly/${idea.id}`} underline="hover" c="inherit">
                              {idea.title}
                            </Anchor>
                          </Title>
                          <Group gap="xs">
                            {changed && (
                              <Badge
                                color="indigo.9"
                                variant="filled"
                                size="lg"
                                radius="md"
                                leftSection={<IconRefreshAlert size={16} aria-hidden="true" />}
                                styles={{ root: { textTransform: 'none', fontSize: '0.95rem' } }}
                              >
                                Zmiana od ostatniej wizyty
                              </Badge>
                            )}
                            <IdeaStatusBadge status={idea.status} />
                          </Group>
                        </Group>
                        <Text c="dimmed" size="sm">
                          Zgłoszono: <time dateTime={idea.createdAt}>{formatDate(idea.createdAt)}</time> · Etap:{' '}
                          {INNOVATION_STAGE_NAMES[idea.stage] ?? idea.stage} · Dla:{' '}
                          {idea.targetGroups.map((g) => TARGET_GROUP_NAMES[g] ?? g).join(', ')}
                        </Text>
                        {idea.adminComment && (
                          <Text>
                            <strong>Komentarz ROPS:</strong> {idea.adminComment}
                          </Text>
                        )}
                        <Group>
                          <Button component={Link} to={`/pomysly/${idea.id}`} variant="light" size="sm">
                            Szczegóły pomysłu
                            <VisuallyHidden component="span"> „{idea.title}”</VisuallyHidden>
                          </Button>
                        </Group>
                      </Stack>
                    </Card>
                  </li>
                )
              })}
            </Stack>
          )}
          <AccessiblePagination
            total={Math.max(1, Math.ceil(total / PAGE_SIZE))}
            value={page + 1}
            onChange={(next) => setPage(next - 1)}
            totalCount={total}
            itemsPerPage={PAGE_SIZE}
            ariaLabel="Paginacja listy pomysłów"
          />
        </>
      )}
    </Stack>
  )
}
