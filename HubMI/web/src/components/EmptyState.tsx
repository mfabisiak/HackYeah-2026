import type { ReactNode } from 'react'
import { Center, Paper, Stack, Text, Title } from '@mantine/core'
import { IconInbox } from '@tabler/icons-react'

export interface EmptyStateProps {
  title?: string
  description?: string
  icon?: ReactNode
  action?: ReactNode
}

export function EmptyState({
  title = 'Brak danych do wyświetlenia',
  description,
  icon,
  action,
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
            backgroundColor: 'var(--mantine-color-gray-1)',
            color: 'var(--mantine-color-gray-6)',
          }}
          aria-hidden="true"
        >
          {icon ?? <IconInbox size={32} />}
        </Center>
        <Title order={3} size="h4" ta="center">
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
