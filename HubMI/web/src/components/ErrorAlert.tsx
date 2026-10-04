import { Alert, Button, Group, Text } from '@mantine/core'
import { IconAlertCircle, IconRefresh } from '@tabler/icons-react'

export interface ErrorAlertProps {
  title?: string
  message: string
  onRetry?: () => void
  retryLabel?: string
}

export function ErrorAlert({
  title = 'Wystąpił błąd',
  message,
  onRetry,
  retryLabel = 'Spróbuj ponownie',
}: ErrorAlertProps) {
  let displayMessage = message
  const lower = (message || '').toLowerCase()
  if (
    lower.includes('failed to fetch') ||
    lower.includes('networkerror') ||
    lower.includes('network error') ||
    lower.includes('load failed')
  ) {
    displayMessage = 'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.'
  }

  return (
    <Alert
      variant="light"
      color="red"
      title={title}
      icon={<IconAlertCircle size={20} aria-hidden="true" />}
      role="alert"
      data-testid="error-alert"
      mb="md"
    >
      <Text size="sm">{displayMessage}</Text>
      {onRetry && (
        <Group mt="sm">
          <Button
            size="xs"
            variant="outline"
            color="red"
            leftSection={<IconRefresh size={14} aria-hidden="true" />}
            onClick={onRetry}
          >
            {retryLabel}
          </Button>
        </Group>
      )}
    </Alert>
  )
}
