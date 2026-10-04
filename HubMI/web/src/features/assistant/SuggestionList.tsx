import { Anchor, Card, Group, Stack, Text, ThemeIcon, Title } from '@mantine/core'
import { IconArrowRight } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import type { SimilarInnovationJs, SuggestionJs } from 'hubmi-client'

export interface SuggestionListProps {
  title: string
  suggestions: SuggestionJs[]
  /** The innovations that suggestions can refer to, to show their titles instead of ids. */
  similar: SimilarInnovationJs[]
}

export function SuggestionList({ title, suggestions, similar }: SuggestionListProps) {
  const titles = new Map(similar.map((item) => [item.innovation.id, item.innovation.title]))

  return (
    <Stack gap="md" component="section" aria-labelledby="suggestions-heading">
      <Title id="suggestions-heading" order={3} size="h4">
        {title}
      </Title>
      <Stack component="ol" gap="sm" p={0} m={0} style={{ listStyle: 'none' }}>
        {suggestions.map((suggestion, index) => (
          <li key={`${index}-${suggestion.title}`}>
            <Card withBorder padding="md" radius="md">
              <Stack gap={8}>
                <Group gap="sm" wrap="nowrap" align="flex-start">
                  <ThemeIcon variant="light" size="lg" radius="xl" aria-hidden="true">
                    {index + 1}
                  </ThemeIcon>
                  <Text fw={700} size="lg">
                    {suggestion.title}
                  </Text>
                </Group>
                <Text size="md">{suggestion.rationale}</Text>
                <Group gap={6} align="flex-start" wrap="nowrap">
                  <IconArrowRight size={18} aria-hidden="true" style={{ marginTop: 3, flexShrink: 0 }} />
                  <Text size="md">
                    <strong>Pierwszy krok:</strong> {suggestion.nextStep}
                  </Text>
                </Group>
                {suggestion.basedOnInnovationIds.length > 0 && (
                  <Text size="sm" c="dimmed">
                    Na podstawie:{' '}
                    {suggestion.basedOnInnovationIds.map((id, i) => (
                      <span key={id}>
                        {i > 0 && ', '}
                        <Anchor component={Link} to={`/innowacje/${id}`}>
                          {titles.get(id) ?? 'innowacja z bazy'}
                        </Anchor>
                      </span>
                    ))}
                  </Text>
                )}
              </Stack>
            </Card>
          </li>
        ))}
      </Stack>
    </Stack>
  )
}
