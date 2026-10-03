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
      <Text size="sm">{message}</Text>
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
