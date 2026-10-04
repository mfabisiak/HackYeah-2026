import { useEffect, useRef } from 'react'
import { Alert, Button, Group, Stack, Text, Title } from '@mantine/core'
import { IconCircleCheck } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { ApplicationSummary } from './ApplicationSummary'
import { formatDateTime } from '../../components/dates'
import type { ApplicationForm } from './form'

interface SubmittedViewProps {
  applicationId: string
  submittedAt: string | null
  form: ApplicationForm
  /** True right after submitting: the confirmation takes focus and is announced. */
  justSubmitted: boolean
}

export function SubmittedView({ applicationId, submittedAt, form, justSubmitted }: SubmittedViewProps) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    if (justSubmitted) headingRef.current?.focus()
  }, [justSubmitted])

  return (
    <Stack gap="lg">
      <Alert
        color="teal"
        variant="light"
        icon={<IconCircleCheck size={28} aria-hidden="true" />}
        title={
          <Title order={2} size="h3" ref={headingRef} tabIndex={-1} style={{ outline: 'none' }}>
            Wniosek został złożony
          </Title>
        }
        role={justSubmitted ? 'status' : 'note'}
      >
        <Stack gap="xs">
          <Text>
            Numer wniosku: <strong>{applicationId}</strong>
          </Text>
          {submittedAt && (
            <Text>
              Data złożenia: <time dateTime={submittedAt}>{formatDateTime(submittedAt)}</time>
            </Text>
          )}
          <Text>
            Złożony wniosek jest tylko do odczytu. Zachowaj numer wniosku – przyda się w kontakcie z ROPS.
          </Text>
        </Stack>
      </Alert>
      <Group>
        <Button component={Link} to="/wnioski">
          Przejdź do moich wniosków
        </Button>
        <Button component={Link} to="/nabory" variant="default">
          Zobacz nabory
        </Button>
      </Group>
      <Title order={2} size="h3">
        Treść złożonego wniosku
      </Title>
      <ApplicationSummary form={form} />
    </Stack>
  )
}
