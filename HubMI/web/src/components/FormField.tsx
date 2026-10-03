import { cloneElement, isValidElement, useId, type ReactElement, type ReactNode } from 'react'
import { Box, Stack, Text } from '@mantine/core'

export interface AccessibleInputProps {
  id: string
  'aria-describedby'?: string
  'aria-invalid'?: boolean
  'aria-required'?: boolean
  required?: boolean
}

export interface FormFieldProps {
  id?: string
  label: string
  description?: string
  error?: string
  required?: boolean
  children: ReactElement<AccessibleInputProps> | ((props: AccessibleInputProps) => ReactNode)
}

export function FormField({
  id: explicitId,
  label,
  description,
  error,
  required,
  children,
}: FormFieldProps) {
  const generatedId = useId()
  const id = explicitId ?? generatedId
  const descriptionId = description ? `${id}-description` : undefined
  const errorId = error ? `${id}-error` : undefined

  const describedBy = [descriptionId, errorId].filter(Boolean).join(' ') || undefined

  const inputProps: AccessibleInputProps = {
    id,
    'aria-describedby': describedBy,
    'aria-invalid': Boolean(error),
    'aria-required': required,
    required,
  }

  return (
    <Stack gap={4} mb="sm" component="div">
      <Box component="label" htmlFor={id} style={{ fontWeight: 500, fontSize: '0.875rem' }}>
        {label}
        {required && (
          <Text component="span" c="red" ml={4} aria-hidden="true">
            *
          </Text>
        )}
      </Box>

      {description && (
        <Text id={descriptionId} size="xs" c="dimmed">
          {description}
        </Text>
      )}

      {typeof children === 'function'
        ? children(inputProps)
        : isValidElement(children)
          ? cloneElement(children, inputProps)
          : children}

      {error && (
        <Text id={errorId} size="xs" c="red" role="alert">
          {error}
        </Text>
      )}
    </Stack>
  )
}
