import { useEffect, useRef } from 'react'
import { Anchor, List, Paper, Stack, Title } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'

export interface FormErrorItem {
  fieldId: string
  message: string
}

export interface ErrorSummaryProps {
  title?: string
  errors: FormErrorItem[]
}

export function ErrorSummary({
  title = 'W formularzu występują błędy',
  errors,
}: ErrorSummaryProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (errors.length > 0) {
      containerRef.current?.focus()
    }
  }, [errors])

  if (errors.length === 0) return null

  const handleLinkClick = (fieldId: string) => (event: React.MouseEvent) => {
    event.preventDefault()
    const element = document.getElementById(fieldId)
    if (element) {
      element.focus()
      element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  return (
    <Paper
      ref={containerRef}
      tabIndex={-1}
      role="alert"
      aria-labelledby="error-summary-title"
      withBorder
      p="md"
      radius="md"
      mb="lg"
      style={{
        borderLeftWidth: 4,
        borderLeftColor: 'var(--mantine-color-red-filled)',
        outline: 'none',
      }}
      data-testid="error-summary"
    >
      <Stack gap="xs">
        <Title
          id="error-summary-title"
          order={3}
          size="h4"
          c="red.7"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <IconAlertTriangle size={20} aria-hidden="true" />
          {title}
        </Title>
        <List size="sm" withPadding>
          {errors.map((err) => (
            <List.Item key={err.fieldId}>
              <Anchor
                href={`#${err.fieldId}`}
                c="red.7"
                fw={500}
                underline="hover"
                onClick={handleLinkClick(err.fieldId)}
              >
                {err.message}
              </Anchor>
            </List.Item>
          ))}
        </List>
      </Stack>
    </Paper>
  )
}
