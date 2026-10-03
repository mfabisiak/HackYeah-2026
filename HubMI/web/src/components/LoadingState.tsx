import { Center, Loader, Stack, Text } from '@mantine/core'

export interface LoadingStateProps {
  message?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  minHeight?: number | string
}

export function LoadingState({
  message = 'Ładowanie danych...',
  size = 'md',
  minHeight = 200,
}: LoadingStateProps) {
  return (
    <Center
      role="status"
      aria-live="polite"
      style={{ minHeight, width: '100%' }}
      data-testid="loading-state"
    >
      <Stack align="center" gap="sm">
        <Loader size={size} aria-hidden="true" />
        <Text size="sm" c="dimmed">
          {message}
        </Text>
      </Stack>
    </Center>
  )
}
