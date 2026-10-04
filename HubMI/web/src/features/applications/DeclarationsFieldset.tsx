import { ERROR_TEXT_COLOR } from '../../components/formStyles'
import { Alert, Button, Checkbox, Fieldset, Stack, Text } from '@mantine/core'
import { IconInfoCircle } from '@tabler/icons-react'
import { useFieldError } from './fields'
import { fieldId } from './steps'

export interface DeclarationOption {
  id: string
  text: string
  required: boolean
}

interface DeclarationsFieldsetProps {
  /** `null` until the applicant type is chosen. */
  declarations: readonly DeclarationOption[] | null
  accepted: readonly string[]
  onChange: (accepted: string[]) => void
  onChooseApplicant: () => void
}

/** Each declaration is a separate checkbox with its full text; there is deliberately no “select all”. */
export function DeclarationsFieldset({ declarations, accepted, onChange, onChooseApplicant }: DeclarationsFieldsetProps) {
  const error = useFieldError('declarations')

  if (declarations === null) {
    return (
      <Alert color="blue" variant="light" role="note" icon={<IconInfoCircle size={22} aria-hidden="true" />}>
        <Stack gap="xs" align="flex-start">
          <Text>Oświadczenia zależą od rodzaju wnioskodawcy. Najpierw wybierz, kto składa wniosek.</Text>
          <Button variant="light" onClick={onChooseApplicant}>
            Przejdź do kroku „Wnioskodawca”
          </Button>
        </Stack>
      </Alert>
    )
  }

  const toggle = (id: string, checked: boolean) =>
    onChange(checked ? [...accepted, id] : accepted.filter((a) => a !== id))

  return (
    <Fieldset legend="Oświadczenia" styles={{ legend: { fontSize: '1.15rem', fontWeight: 700 } }}>
      <Stack gap="md">
        <Text>Zaznacz każde oświadczenie osobno. Wszystkie są wymagane do złożenia wniosku.</Text>
        {declarations.map((declaration, index) => (
          <Checkbox
            key={declaration.id}
            id={index === 0 ? fieldId('declarations') : undefined}
            checked={accepted.includes(declaration.id)}
            onChange={(event) => toggle(declaration.id, event.currentTarget.checked)}
            label={declaration.text}
            size="md"
            styles={{ label: { fontSize: '1rem', lineHeight: 1.5 } }}
          />
        ))}
        {error && (
          <Text c={ERROR_TEXT_COLOR} fw={600} role="alert">
            {error}
          </Text>
        )}
      </Stack>
    </Fieldset>
  )
}
