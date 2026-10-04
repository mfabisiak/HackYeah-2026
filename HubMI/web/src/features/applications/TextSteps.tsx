import { Button, Group, NativeSelect, Paper, Stack, Text, VisuallyHidden } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { INPUT_STYLES } from '../../components/formStyles'
import { SOCIAL_AREA_OPTIONS } from '../knowledge/constants'
import { NarrativeField, TextField, useFieldError } from './fields'
import {
  MAX_TITLE_LENGTH,
  formatGrosze,
  groszeToInput,
  parsePln,
  planTotalGrosze,
  type ApplicationForm,
} from './form'
import { fieldId } from './steps'

interface StepProps {
  form: ApplicationForm
  onChange: (patch: Partial<ApplicationForm>) => void
}

export function TitleStep({ form, onChange }: StepProps) {
  return (
    <Stack gap="md">
      <TextField
        path="title"
        label="Tytuł innowacji"
        description={`Krótka nazwa, po której łatwo rozpoznać pomysł (do ${MAX_TITLE_LENGTH} znaków).`}
        value={form.title}
        onChange={(title) => onChange({ title })}
      />
      <Text size="sm" c="dimmed">
        {form.title.trim().length} / {MAX_TITLE_LENGTH} znaków
      </Text>
    </Stack>
  )
}

export function DescriptionStep({ form, onChange }: StepProps) {
  return (
    <Stack gap="lg">
      <NarrativeField
        path="description"
        label="Opis innowacji"
        description="Na czym polega Twoje rozwiązanie? Co konkretnie będzie się działo?"
        value={form.description}
        onChange={(description) => onChange({ description })}
      />
      <NarrativeField
        path="innovativeness"
        label="Innowacyjność rozwiązania"
        description="Czym to rozwiązanie różni się od tego, co już jest dostępne w Małopolsce?"
        value={form.innovativeness}
        onChange={(innovativeness) => onChange({ innovativeness })}
      />
    </Stack>
  )
}

export function DiagnosisStep({ form, onChange }: StepProps) {
  const areaError = useFieldError('socialArea')
  return (
    <Stack gap="lg">
      <NativeSelect
        id={fieldId('socialArea')}
        label="Obszar z Mapy Wyzwań Społecznych"
        description="Wybierz obszar, którego dotyczy problem."
        data={[{ value: '', label: 'Wybierz obszar' }, ...SOCIAL_AREA_OPTIONS]}
        value={form.socialArea}
        onChange={(event) => onChange({ socialArea: event.currentTarget.value })}
        error={areaError}
        required
        size="md"
        styles={INPUT_STYLES}
      />
      <NarrativeField
        path="problemDiagnosis"
        label="Diagnoza problemu"
        description="Jaki problem chcesz rozwiązać? Skąd o nim wiesz i kogo dotyka?"
        value={form.problemDiagnosis}
        onChange={(problemDiagnosis) => onChange({ problemDiagnosis })}
      />
    </Stack>
  )
}

export function AudienceStep({ form, onChange }: StepProps) {
  return (
    <Stack gap="lg">
      <NarrativeField
        path="audienceDescription"
        label="Odbiorcy innowacji"
        description="Kto skorzysta z rozwiązania? Ile to osób i gdzie mieszkają?"
        value={form.audienceDescription}
        onChange={(audienceDescription) => onChange({ audienceDescription })}
        minRows={4}
      />
      <NarrativeField
        path="expectedChange"
        label="Zmiana, jaką wprowadza innowacja"
        description="Co się zmieni w życiu odbiorców po wdrożeniu?"
        value={form.expectedChange}
        onChange={(expectedChange) => onChange({ expectedChange })}
        minRows={4}
      />
      <NarrativeField
        path="futureVision"
        label="Wizja przyszłości innowacji"
        description="Co dalej z rozwiązaniem po zakończeniu testów? Czy można je powtórzyć w innych gminach?"
        value={form.futureVision}
        onChange={(futureVision) => onChange({ futureVision })}
        minRows={4}
      />
    </Stack>
  )
}

function grantMessage(grantText: string, total: number): string {
  const grant = parsePln(grantText)
  if (grant === null) return `Suma kosztów z planu: ${formatGrosze(total)}. Kwota grantu musi być równa tej sumie.`
  if (grant === total) return `Kwota grantu zgadza się z sumą kosztów (${formatGrosze(total)}).`
  const diff = Math.abs(grant - total)
  return `Kwota grantu różni się od sumy kosztów o ${formatGrosze(diff)} (suma kosztów z planu: ${formatGrosze(total)}).`
}

export function BudgetStep({ form, onChange }: StepProps) {
  const total = planTotalGrosze(form.plan)
  const message = grantMessage(form.requestedGrant, total)
  const [announced] = useDebouncedValue(message, 900)
  const matches = parsePln(form.requestedGrant) === total
  return (
    <Stack gap="lg">
      <Stack gap="xs">
        <TextField
          path="requestedGrantAmountGrosze"
          label="Wnioskowana kwota grantu (zł)"
          description="Musi być równa sumie kosztów z planu działania."
          value={form.requestedGrant}
          onChange={(requestedGrant) => onChange({ requestedGrant })}
          inputMode="decimal"
          autoComplete="off"
        />
        <Paper withBorder p="sm" radius="md">
          <Group justify="space-between" align="center" gap="sm">
            <Text fw={matches ? 700 : 500}>{message}</Text>
            {!matches && total > 0 && (
              <Button
                variant="light"
                size="sm"
                onClick={() => {
                  onChange({ requestedGrant: groszeToInput(total) })
                  // The button disappears once the amounts match, so keep focus on the field it fills.
                  document.getElementById(fieldId('requestedGrantAmountGrosze'))?.focus()
                }}
              >
                Wpisz sumę kosztów ({formatGrosze(total)})
              </Button>
            )}
          </Group>
        </Paper>
        <VisuallyHidden role="status">{announced}</VisuallyHidden>
      </Stack>
      <NarrativeField
        path="projectTeam"
        label="Zespół projektowy"
        description="Kto będzie realizował projekt i jakie ma doświadczenie? Nie podawaj numerów PESEL ani adresów prywatnych."
        value={form.projectTeam}
        onChange={(projectTeam) => onChange({ projectTeam })}
        minRows={4}
      />
    </Stack>
  )
}
