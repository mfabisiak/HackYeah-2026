import { Badge, Card, Group, Paper, Stack, Text, Title } from '@mantine/core'
import { IconUsers } from '@tabler/icons-react'
import { SOCIAL_AREA_NAMES } from './constants'
import type { SimilarNeedItem } from './types'

export interface SimilarNeedsListProps {
  items: SimilarNeedItem[]
}

export function SimilarNeedsList({ items }: SimilarNeedsListProps) {
  if (!items || items.length === 0) return null

  return (
    <Paper
      withBorder
      p="md"
      radius="md"
      component="section"
      aria-labelledby="similar-needs-heading"
    >
      <Stack gap="sm">
        <Group gap="xs">
          <IconUsers size={20} color="var(--mantine-color-blue-filled)" aria-hidden="true" />
          <Title order={3} size="h4" id="similar-needs-heading">
            Podobne wyzwania zgłoszone w Małopolsce
          </Title>
        </Group>

        <Text size="xs" c="dimmed">
          Inni mieszkańcy i samorządowcy mierzyli się z podobnymi tematami:
        </Text>

        <Stack component="ul" gap="xs" p={0} m={0} style={{ listStyle: 'none' }}>
          {items.map((item) => (
            <li key={item.id}>
              <Card withBorder padding="sm" radius="sm">
                <Group justify="space-between" align="center" wrap="wrap" gap="xs">
                  <Text size="sm" style={{ flex: 1, minWidth: 200 }}>
                    „{item.excerpt}”
                  </Text>
                  {item.area && (
                    <Badge variant="light" color="blue" size="xs">
                      {SOCIAL_AREA_NAMES[item.area] ?? item.area}
                    </Badge>
                  )}
                </Group>
              </Card>
            </li>
          ))}
        </Stack>
      </Stack>
    </Paper>
  )
}
