import { createContext, useContext, type ReactNode } from 'react'
import { Group, Text, TextInput, Textarea } from '@mantine/core'
import { ERROR_TEXT_COLOR, INPUT_STYLES } from '../../components/formStyles'
import { MAX_NARRATIVE_LENGTH, type AddressForm } from './form'
import { fieldId } from './steps'

/** Error message for a field path (empty when the field is fine); provided by the wizard. */
const IssuesContext = createContext<(path: string) => string | undefined>(() => undefined)

export function IssuesProvider({
  errorFor,
  children,
}: {
  errorFor: (path: string) => string | undefined
  children: ReactNode
}) {
  return <IssuesContext value={errorFor}>{children}</IssuesContext>
}

export const useErrorLookup = (): ((path: string) => string | undefined) => useContext(IssuesContext)

export const useFieldError = (path: string): string | undefined => useErrorLookup()(path)

interface TextFieldProps {
  path: string
  label: string
  value: string
  onChange: (value: string) => void
  description?: string
  autoComplete?: string
  inputMode?: 'text' | 'numeric' | 'decimal' | 'tel' | 'email'
  type?: 'text' | 'email' | 'tel'
  optional?: boolean
  placeholder?: string
  readOnly?: boolean
}

export function TextField({
  path,
  label,
  value,
  onChange,
  description,
  autoComplete,
  inputMode,
  type = 'text',
  optional,
  placeholder,
  readOnly,
}: TextFieldProps) {
  return (
    <TextInput
      id={fieldId(path)}
      label={label}
      description={description}
      value={value}
      onChange={(event) => onChange(event.currentTarget.value)}
      error={useFieldError(path)}
      autoComplete={autoComplete}
      inputMode={inputMode}
      type={type}
      required={!optional}
      placeholder={placeholder}
      readOnly={readOnly}
      size="md"
      styles={INPUT_STYLES}
    />
  )
}

interface NarrativeFieldProps {
  path: string
  label: string
  description: string
  value: string
  onChange: (value: string) => void
  minRows?: number
}

export function NarrativeField({ path, label, description, value, onChange, minRows = 6 }: NarrativeFieldProps) {
  const length = value.trim().length
  const over = length - MAX_NARRATIVE_LENGTH
  return (
    <div>
      <Textarea
        id={fieldId(path)}
        label={label}
        description={description}
        value={value}
        onChange={(event) => onChange(event.currentTarget.value)}
        error={useFieldError(path)}
        required
        autosize
        minRows={minRows}
        maxRows={20}
        size="md"
        styles={INPUT_STYLES}
      />
      <Group justify="flex-end" mt={4}>
        <Text size="sm" c={over > 0 ? ERROR_TEXT_COLOR : 'dimmed'} fw={over > 0 ? 700 : 400}>
          {over > 0
            ? `Przekroczono limit o ${over} znaków (${length} / ${MAX_NARRATIVE_LENGTH})`
            : `${length} / ${MAX_NARRATIVE_LENGTH} znaków`}
        </Text>
      </Group>
    </div>
  )
}

interface AddressFieldsProps {
  prefix: string
  value: AddressForm
  onChange: (value: AddressForm) => void
  /** Browser autofill is offered only for the applicant's own data. */
  autofill?: boolean
}

export function AddressFields({ prefix, value, onChange, autofill }: AddressFieldsProps) {
  const set = (patch: Partial<AddressForm>) => onChange({ ...value, ...patch })
  return (
    <>
      <TextField
        path={`${prefix}.street`}
        label="Ulica"
        value={value.street}
        onChange={(street) => set({ street })}
        autoComplete={autofill ? 'address-line1' : 'off'}
      />
      <Group grow align="flex-start">
        <TextField
          path={`${prefix}.buildingNumber`}
          label="Numer budynku"
          value={value.buildingNumber}
          onChange={(buildingNumber) => set({ buildingNumber })}
          autoComplete="off"
        />
        <TextField
          path={`${prefix}.apartmentNumber`}
          label="Numer lokalu"
          value={value.apartmentNumber}
          onChange={(apartmentNumber) => set({ apartmentNumber })}
          optional
          autoComplete="off"
        />
      </Group>
      <Group grow align="flex-start">
        <TextField
          path={`${prefix}.postalCode`}
          label="Kod pocztowy"
          description="Format: 30-001"
          value={value.postalCode}
          onChange={(postalCode) => set({ postalCode })}
          autoComplete={autofill ? 'postal-code' : 'off'}
        />
        <TextField
          path={`${prefix}.city`}
          label="Miejscowość"
          value={value.city}
          onChange={(city) => set({ city })}
          autoComplete={autofill ? 'address-level2' : 'off'}
        />
      </Group>
    </>
  )
}
