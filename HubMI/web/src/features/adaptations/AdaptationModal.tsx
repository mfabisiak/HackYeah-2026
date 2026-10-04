import { useState } from 'react'
import { Alert, Anchor, Button, Group, Loader, Modal, NumberInput, Select, Stack, Text, Textarea } from '@mantine/core'
import { IconCircleCheck, IconPlayerStop, IconSparkles } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { ErrorAlert } from '../../components/ErrorAlert'
import { ErrorSummary, type FormErrorItem } from '../../components/ErrorSummary'
import {
  ADAPTATION_AI_NOTICES,
  BUDGET_RANGE,
  INSTITUTION_TYPE_OPTIONS,
  MAX_CONTEXT_LENGTH,
  STAFF_RANGE,
} from './constants'
import { AdaptationPlanView, AdaptationSteps } from './AdaptationPlanView'
import { validateInstitution, type InstitutionErrors } from './institutionValidation'
import { useAdaptationStream } from './useAdaptationStream'

export interface AdaptationModalProps {
  opened: boolean
  onClose: () => void
  innovationId: string
  innovationTitle: string
}

const FIELD_IDS = {
  type: 'adaptation-type',
  staffCount: 'adaptation-staff',
  budgetPln: 'adaptation-budget',
  context: 'adaptation-context',
} as const

function summaryOf(errors: InstitutionErrors): FormErrorItem[] {
  return (Object.keys(FIELD_IDS) as (keyof typeof FIELD_IDS)[]).flatMap((field) => {
    const message = errors[field]
    return message ? [{ fieldId: FIELD_IDS[field], message }] : []
  })
}

/** The Middleman: how could this innovation be run by my institution? The plan is written live and kept for ROPS to review. */
export function AdaptationModal({ opened, onClose, innovationId, innovationTitle }: AdaptationModalProps) {
  const { authenticated, login } = useAuth()
  const { state, run, stop, reset } = useAdaptationStream()
  const [type, setType] = useState<string | null>(null)
  const [staffCount, setStaffCount] = useState<number | string>(3)
  const [budgetPln, setBudgetPln] = useState<number | string>(50_000)
  const [context, setContext] = useState('')
  const [errors, setErrors] = useState<InstitutionErrors>({})

  const close = () => {
    reset()
    onClose()
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const result = validateInstitution({ type, staffCount, budgetPln, context })
    setErrors(result.errors)
    if (result.institution) run(innovationId, result.institution)
  }

  const adaptation = state.response?.adaptation ?? null
  const showsForm = state.phase === 'idle' || state.phase === 'stopped' || state.phase === 'failed' ||
    (state.phase === 'done' && !adaptation)

  return (
    <Modal
      opened={opened}
      onClose={close}
      title="Dostosuj innowację do swojej instytucji"
      size="xl"
      radius="md"
      trapFocus
      returnFocus
      closeOnEscape
      styles={{ title: { fontSize: '1.4rem', fontWeight: 700 } }}
    >
      <Stack gap="lg">
        <Text size="lg">
          Innowacja: <strong>{innovationTitle}</strong>
        </Text>

        {!authenticated ? (
          <Alert color="blue" title="Wymagane logowanie">
            <Stack gap="md">
              <Text size="md">Aby poprosić asystenta o plan, musisz być zalogowany na platformie.</Text>
              <div>
                <Button onClick={login} size="lg">
                  Zaloguj się
                </Button>
              </div>
            </Stack>
          </Alert>
        ) : (
          <Stack gap="lg" role="region" aria-label="Plan adaptacji" aria-live="polite" aria-busy={state.phase === 'running'}>
            {state.phase === 'running' && (
              <Stack gap="md">
                <Group gap="sm" data-testid="adaptation-working">
                  <Loader size="sm" aria-hidden="true" />
                  <Text size="md">Asystent układa plan pilotażu… Kroki pojawiają się na bieżąco.</Text>
                  <Button
                    size="xs"
                    variant="default"
                    leftSection={<IconPlayerStop size={14} aria-hidden="true" />}
                    onClick={stop}
                  >
                    Przerwij
                  </Button>
                </Group>
                {state.steps.length > 0 && <AdaptationSteps steps={state.steps} />}
              </Stack>
            )}

            {adaptation && (
              <Stack gap="lg">
                <Alert
                  color="teal"
                  variant="light"
                  icon={<IconCircleCheck size={24} aria-hidden="true" />}
                  title="Plan zapisany"
                >
                  <Text size="md">
                    Plan czeka na przegląd pracownika ROPS. Wrócisz do niego w sekcji{' '}
                    <Anchor component={Link} to="/moje-plany" onClick={close}>
                      Moje plany adaptacji
                    </Anchor>
                    .
                  </Text>
                </Alert>
                <AdaptationPlanView adaptation={adaptation} />
                <Group justify="flex-end">
                  <Button size="lg" variant="default" onClick={close}>
                    Zamknij
                  </Button>
                </Group>
              </Stack>
            )}

            {state.phase === 'stopped' && (
              <Text size="md" c="dimmed">
                Przerwano, nic nie zostało zapisane. Możesz zmienić dane i spróbować ponownie.
              </Text>
            )}

            {state.phase === 'done' && !adaptation && state.response && (
              <ErrorAlert
                title="Plan nie powstał"
                message={ADAPTATION_AI_NOTICES[state.response.aiStatus] ?? 'Nie udało się przygotować planu.'}
              />
            )}

            {state.phase === 'failed' && state.error && (
              <ErrorAlert
                title="Nie udało się"
                message={state.error}
                onRetry={state.unauthorized ? login : undefined}
                retryLabel="Zaloguj się ponownie"
              />
            )}

            {showsForm && (
              <form onSubmit={submit} noValidate>
                <Stack gap="lg">
                  <ErrorSummary errors={summaryOf(errors)} />

                  <Select
                    id={FIELD_IDS.type}
                    label="Kim jesteś?"
                    placeholder="Wybierz rodzaj instytucji"
                    data={[...INSTITUTION_TYPE_OPTIONS]}
                    value={type}
                    onChange={setType}
                    error={errors.type}
                    allowDeselect={false}
                    required
                    size="lg"
                  />

                  <NumberInput
                    id={FIELD_IDS.staffCount}
                    label="Ile osób może zająć się usługą?"
                    description="Pracownicy lub wolontariusze, których możesz do tego wyznaczyć."
                    value={staffCount}
                    onChange={setStaffCount}
                    error={errors.staffCount}
                    min={STAFF_RANGE.min}
                    max={STAFF_RANGE.max}
                    allowDecimal={false}
                    allowNegative={false}
                    required
                    size="lg"
                  />

                  <NumberInput
                    id={FIELD_IDS.budgetPln}
                    label="Ile możesz wydać na pilotaż? (w złotych)"
                    value={budgetPln}
                    onChange={setBudgetPln}
                    error={errors.budgetPln}
                    min={BUDGET_RANGE.min}
                    max={BUDGET_RANGE.max}
                    allowDecimal={false}
                    allowNegative={false}
                    thousandSeparator=" "
                    suffix=" zł"
                    required
                    size="lg"
                  />

                  <Textarea
                    id={FIELD_IDS.context}
                    label="Jakie są warunki u Was?"
                    description="Np. wielkość gminy, co już działa, czego brakuje. Bez imion i danych osobowych."
                    value={context}
                    onChange={(e) => setContext(e.currentTarget.value)}
                    error={errors.context}
                    minRows={4}
                    autosize
                    maxLength={MAX_CONTEXT_LENGTH + 100}
                    required
                    size="lg"
                  />
                  <Text size="sm" c="dimmed" aria-live="off">
                    {context.trim().length} z {MAX_CONTEXT_LENGTH} znaków
                  </Text>

                  <Group justify="flex-end" gap="md">
                    <Button variant="default" size="lg" onClick={close}>
                      Zamknij
                    </Button>
                    <Button type="submit" size="lg" color="grape" leftSection={<IconSparkles size={20} aria-hidden="true" />}>
                      Przygotuj plan
                    </Button>
                  </Group>
                </Stack>
              </form>
            )}
          </Stack>
        )}
      </Stack>
    </Modal>
  )
}
