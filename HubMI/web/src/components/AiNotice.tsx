import { Alert, Text } from '@mantine/core'
import { IconSparkles } from '@tabler/icons-react'
import type { ReactNode } from 'react'

export interface AiNoticeProps {
  children?: ReactNode
}

/** Marks everything below it as written by the local AI model, which is what the API's `aiGenerated` stands for. */
export function AiNotice({ children }: AiNoticeProps) {
  return (
    <Alert
      variant="light"
      color="grape"
      icon={<IconSparkles size={22} aria-hidden="true" />}
      title="Wygenerowane przez AI"
      data-testid="ai-notice"
    >
      <Text size="md">
        {children ?? 'To propozycja przygotowana przez asystenta AI. Traktuj ją jako punkt wyjścia i sprawdź przed użyciem.'}
      </Text>
    </Alert>
  )
}
