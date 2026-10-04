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
  /** Replaces the default “focus the field” behaviour, e.g. when the field is on another step of a wizard. */
  onNavigate?: (fieldId: string) => void
  /** When given, focus moves to the summary only if this value changes, not whenever `errors` is re-created. */
  focusKey?: number
}

export function ErrorSummary({
  title = 'W formularzu występują błędy',
  errors,
  onNavigate,
  focusKey,
}: ErrorSummaryProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  const hasErrors = errors.length > 0
  useEffect(() => {
    if (hasErrors) {
      containerRef.current?.focus()
    }
  }, [focusKey ?? errors, hasErrors]) // eslint-disable-line react-hooks/exhaustive-deps

  if (errors.length === 0) return null

  const handleLinkClick = (fieldId: string) => (event: React.MouseEvent) => {
    event.preventDefault()
    if (onNavigate) {
      onNavigate(fieldId)
      return
    }
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
