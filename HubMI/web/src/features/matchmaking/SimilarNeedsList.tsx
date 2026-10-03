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
      p={{ base: 'md', sm: 'xl' }}
      radius="lg"
      shadow="xs"
      component="section"
      aria-labelledby="similar-needs-heading"
    >
      <Stack gap="md">
        <Group gap="sm">
          <IconUsers size={24} color="var(--mantine-color-blue-filled)" aria-hidden="true" />
          <Title order={3} size="h3" id="similar-needs-heading">
            Podobne wyzwania zgłoszone w Małopolsce
          </Title>
        </Group>

        <Text
          size="md"
          c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
        >
          Zobacz, o jakich zbliżonych sytuacjach informowali nas inni mieszkańcy i samorządowcy z regionu:
        </Text>

        <Stack component="ul" gap="sm" p={0} m={0} style={{ listStyle: 'none' }}>
          {items.map((item) => (
            <li key={item.id}>
              <Card withBorder padding="md" radius="md">
                <Group justify="space-between" align="center" wrap="wrap" gap="sm">
                  <Text size="md" style={{ flex: 1, minWidth: 240, fontSize: '1.05rem', lineHeight: 1.5 }}>
                    „{item.excerpt}”
                  </Text>
                  {item.area && (
                    <Badge variant="light" color="blue" size="md" radius="sm">
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
