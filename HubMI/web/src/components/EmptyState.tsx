import type { ReactNode } from 'react'
import { Center, Paper, Stack, Text, Title } from '@mantine/core'
import { IconInbox } from '@tabler/icons-react'

export interface EmptyStateProps {
  title?: string
  description?: string
  icon?: ReactNode
  action?: ReactNode
  /** Heading level of the title; the default fits a page whose title is the `h1`. */
  titleOrder?: 2 | 3 | 4
}

export function EmptyState({
  title = 'Brak danych do wyświetlenia',
  description,
  icon,
  action,
  titleOrder = 2,
}: EmptyStateProps) {
  return (
    <Paper
      withBorder
      p="xl"
      radius="md"
      role="region"
      aria-label={title}
      data-testid="empty-state"
    >
      <Stack align="center" gap="sm" py="md">
        <Center
          style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            backgroundColor: 'light-dark(var(--mantine-color-gray-1), var(--mantine-color-dark-5))',
            color: 'light-dark(var(--mantine-color-gray-7), var(--mantine-color-gray-3))',
          }}
          aria-hidden="true"
        >
          {icon ?? <IconInbox size={32} />}
        </Center>
        <Title order={titleOrder} size="h4" ta="center">
          {title}
        </Title>
        {description && (
          <Text c="dimmed" size="sm" ta="center" maw={480}>
            {description}
          </Text>
        )}
        {action && <div style={{ marginTop: '0.5rem' }}>{action}</div>}
      </Stack>
    </Paper>
  )
}
