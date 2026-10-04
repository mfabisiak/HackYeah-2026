import { ERROR_TEXT_COLOR } from '../../components/formStyles'
import { useState } from 'react'
import { flushSync } from 'react-dom'
import { Alert, Button, Fieldset, Group, Modal, Radio, Stack, Text } from '@mantine/core'
import { IconInfoCircle, IconPlus, IconTrash } from '@tabler/icons-react'
import { AddressFields, TextField, useFieldError } from './fields'
import { useAnnouncer } from './announcer'
import {
  MAX_PARTNERS,
  applicantHasData,
  emptyApplicant,
  emptyPartner,
  type ApplicantForm,
  type ApplicantType,
  type ContactPersonForm,
  type EntityApplicantForm,
  type GroupApplicantForm,
  type GroupRepresentativeForm,
  type IndividualApplicantForm,
  type PartnerForm,
} from './form'
import { fieldId } from './steps'

export const APPLICANT_TYPE_LABELS: Record<ApplicantType, string> = {
  INDIVIDUAL: 'Osoba fizyczna',
  ENTITY: 'Podmiot (organizacja, instytucja, firma)',
  NON_FORMAL_GROUP: 'Grupa nieformalna',
}

const APPLICANT_TYPE_HINTS: Record<ApplicantType, string> = {
  INDIVIDUAL: 'Składasz wniosek jako osoba prywatna.',
  ENTITY: 'Wniosek składa stowarzyszenie, fundacja, spółdzielnia, urząd, firma lub inny podmiot z KRS.',
  NON_FORMAL_GROUP: 'Wniosek składa grupa od 1 do 5 osób lub podmiotów, która nie ma osobowości prawnej.',
}

interface ApplicantStepProps {
  applicant: ApplicantForm | null
  onChange: (applicant: ApplicantForm) => void
}

export function ApplicantStep({ applicant, onChange }: ApplicantStepProps) {
  const [pendingType, setPendingType] = useState<ApplicantType | null>(null)
  const typeError = useFieldError('applicant')

  const requestType = (type: ApplicantType) => {
    if (applicant?.type === type) return
    if (applicantHasData(applicant)) setPendingType(type)
    else onChange(emptyApplicant(type))
  }

  return (
    <Stack gap="lg">
      <Radio.Group
        label="Kto składa wniosek?"
        value={applicant?.type ?? ''}
        onChange={(value) => requestType(value as ApplicantType)}
        error={typeError}
        required
        size="md"
        styles={{ label: { fontSize: '1.05rem', fontWeight: 700 } }}
      >
        <Stack gap="sm" mt="xs">
          {(Object.keys(APPLICANT_TYPE_LABELS) as ApplicantType[]).map((type, index) => (
            <Radio
              key={type}
              id={index === 0 ? fieldId('applicant') : undefined}
              value={type}
              label={APPLICANT_TYPE_LABELS[type]}
              description={APPLICANT_TYPE_HINTS[type]}
              size="md"
            />
          ))}
        </Stack>
      </Radio.Group>

      {applicant?.type === 'INDIVIDUAL' && <IndividualFields value={applicant} onChange={onChange} />}
      {applicant?.type === 'ENTITY' && <EntityFields value={applicant} onChange={onChange} />}
      {applicant?.type === 'NON_FORMAL_GROUP' && <GroupFields value={applicant} onChange={onChange} />}

      <Modal
        opened={pendingType !== null}
        onClose={() => setPendingType(null)}
        title="Zmienić rodzaj wnioskodawcy?"
        size="lg"
        trapFocus
        returnFocus
        closeOnEscape
      >
        <Stack gap="md">
          <Text>
            Dane wnioskodawcy wpisane do tej pory (adres, telefon, e-mail i inne) zostaną usunięte, bo każdy rodzaj
            wnioskodawcy ma inny zestaw pól. Oświadczenia zaznaczone w ostatnim kroku trzeba będzie zaznaczyć ponownie.
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setPendingType(null)} data-autofocus>
              Zostaw bez zmian
            </Button>
            <Button
              color="red"
              onClick={() => {
                if (pendingType) onChange(emptyApplicant(pendingType))
                setPendingType(null)
              }}
            >
              Zmień i usuń dane
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}

function Section({ legend, children }: { legend: string; children: React.ReactNode }) {
  return (
    <Fieldset legend={legend} styles={{ legend: { fontSize: '1.1rem', fontWeight: 700 } }}>
      <Stack gap="sm">{children}</Stack>
    </Fieldset>
  )
}

function IndividualFields({
  value,
  onChange,
}: {
  value: IndividualApplicantForm
  onChange: (v: ApplicantForm) => void
}) {
  const set = (patch: Partial<IndividualApplicantForm>) => onChange({ ...value, ...patch })
  return (
    <>
      <Section legend="Dane osoby">
        <TextField
          path="applicant.firstName"
          label="Imię"
          value={value.firstName}
          onChange={(firstName) => set({ firstName })}
          autoComplete="given-name"
        />
        <TextField
          path="applicant.lastName"
          label="Nazwisko"
          value={value.lastName}
          onChange={(lastName) => set({ lastName })}
          autoComplete="family-name"
        />
      </Section>
      <Section legend="Adres zamieszkania">
        <AddressFields
          prefix="applicant.address"
          value={value.address}
          onChange={(address) => set({ address })}
          autofill
        />
      </Section>
      <Section legend="Kontakt">
        <TextField
          path="applicant.phone"
          label="Telefon"
          value={value.phone}
          onChange={(phone) => set({ phone })}
          autoComplete="tel"
          type="tel"
          inputMode="tel"
        />
        <TextField
          path="applicant.email"
          label="Adres e-mail"
          value={value.email}
          onChange={(email) => set({ email })}
          autoComplete="email"
          type="email"
          inputMode="email"
        />
      </Section>
    </>
  )
}

function OrganisationIdentifiers({
  prefix,
  value,
  onChange,
}: {
  prefix: string
  value: { name: string; krs: string; regon: string; nip: string }
  onChange: (patch: Partial<{ name: string; krs: string; regon: string; nip: string }>) => void
}) {
  return (
    <>
      <TextField
        path={`${prefix}.name`}
        label="Nazwa podmiotu"
        value={value.name}
        onChange={(name) => onChange({ name })}
      />
      <TextField
        path={`${prefix}.krs`}
        label="Numer KRS"
        description="10 cyfr, bez spacji"
        value={value.krs}
        onChange={(krs) => onChange({ krs })}
        inputMode="numeric"
      />
      <TextField
        path={`${prefix}.regon`}
        label="REGON"
        description="9 lub 14 cyfr"
        value={value.regon}
        onChange={(regon) => onChange({ regon })}
        inputMode="numeric"
      />
      <TextField
        path={`${prefix}.nip`}
        label="NIP"
        description="10 cyfr, z myślnikami lub bez"
        value={value.nip}
        onChange={(nip) => onChange({ nip })}
        inputMode="numeric"
      />
    </>
  )
}

function ContactPersonFields({
  prefix,
  value,
  onChange,
}: {
  prefix: string
  value: ContactPersonForm
  onChange: (v: ContactPersonForm) => void
}) {
  const set = (patch: Partial<ContactPersonForm>) => onChange({ ...value, ...patch })
  return (
    <>
      <TextField
        path={`${prefix}.fullName`}
        label="Imię i nazwisko"
        value={value.fullName}
        onChange={(fullName) => set({ fullName })}
      />
      <TextField
        path={`${prefix}.function`}
        label="Funkcja lub stanowisko"
        value={value.function}
        onChange={(function_) => set({ function: function_ })}
      />
      <TextField
        path={`${prefix}.phone`}
        label="Telefon"
        value={value.phone}
        onChange={(phone) => set({ phone })}
        type="tel"
        inputMode="tel"
      />
      <TextField
        path={`${prefix}.email`}
        label="Adres e-mail"
        value={value.email}
        onChange={(email) => set({ email })}
        type="email"
        inputMode="email"
      />
    </>
  )
}

function EntityFields({ value, onChange }: { value: EntityApplicantForm; onChange: (v: ApplicantForm) => void }) {
  const set = (patch: Partial<EntityApplicantForm>) => onChange({ ...value, ...patch })
  return (
    <>
      <Section legend="Dane podmiotu">
        <OrganisationIdentifiers prefix="applicant" value={value} onChange={set} />
      </Section>
      <Section legend="Adres siedziby">
        <AddressFields prefix="applicant.address" value={value.address} onChange={(address) => set({ address })} />
      </Section>
      <Section legend="Kontakt do podmiotu">
        <TextField
          path="applicant.phone"
          label="Telefon"
          value={value.phone}
          onChange={(phone) => set({ phone })}
          type="tel"
          inputMode="tel"
        />
        <TextField
          path="applicant.email"
          label="Adres e-mail"
          value={value.email}
          onChange={(email) => set({ email })}
          type="email"
          inputMode="email"
        />
      </Section>
      <Section legend="Osoba upoważniona do reprezentacji podmiotu">
        <ContactPersonFields
          prefix="applicant.representative"
          value={value.representative}
          onChange={(representative) => set({ representative })}
        />
      </Section>
      <Section legend="Osoba do kontaktów roboczych">
        <ContactPersonFields
          prefix="applicant.contactPerson"
          value={value.contactPerson}
          onChange={(contactPerson) => set({ contactPerson })}
        />
      </Section>
    </>
  )
}

function partnerLabel(index: number, partner: PartnerForm): string {
  return `Partner ${index + 1}: ${partner.kind === 'INDIVIDUAL' ? 'osoba' : 'podmiot'}`
}

function PartnerFields({
  index,
  value,
  onChange,
}: {
  index: number
  value: PartnerForm
  onChange: (v: PartnerForm) => void
}) {
  const prefix = `applicant.partners[${index}]`
  if (value.kind === 'INDIVIDUAL') {
    const set = (patch: Partial<typeof value>) => onChange({ ...value, ...patch })
    return (
      <>
        <TextField
          path={`${prefix}.firstName`}
          label="Imię"
          value={value.firstName}
          onChange={(firstName) => set({ firstName })}
        />
        <TextField
          path={`${prefix}.lastName`}
          label="Nazwisko"
          value={value.lastName}
          onChange={(lastName) => set({ lastName })}
        />
        <AddressFields prefix={`${prefix}.address`} value={value.address} onChange={(address) => set({ address })} />
        <TextField
          path={`${prefix}.phone`}
          label="Telefon"
          value={value.phone}
          onChange={(phone) => set({ phone })}
          type="tel"
          inputMode="tel"
        />
        <TextField
          path={`${prefix}.email`}
          label="Adres e-mail"
          value={value.email}
          onChange={(email) => set({ email })}
          type="email"
          inputMode="email"
        />
      </>
    )
  }
  const set = (patch: Partial<typeof value>) => onChange({ ...value, ...patch })
  return (
    <>
      <OrganisationIdentifiers prefix={prefix} value={value} onChange={set} />
      <AddressFields prefix={`${prefix}.address`} value={value.address} onChange={(address) => set({ address })} />
      <TextField
        path={`${prefix}.phone`}
        label="Telefon"
        value={value.phone}
        onChange={(phone) => set({ phone })}
        type="tel"
        inputMode="tel"
      />
      <TextField
        path={`${prefix}.email`}
        label="Adres e-mail"
        value={value.email}
        onChange={(email) => set({ email })}
        type="email"
        inputMode="email"
      />
    </>
  )
}

function GroupFields({ value, onChange }: { value: GroupApplicantForm; onChange: (v: ApplicantForm) => void }) {
  const announce = useAnnouncer()
  const partnersError = useFieldError('applicant.partners')
  const set = (patch: Partial<GroupApplicantForm>) => onChange({ ...value, ...patch })
  const setRepresentative = (patch: Partial<GroupRepresentativeForm>) =>
    set({ representative: { ...value.representative, ...patch } })

  const addPartner = (kind: PartnerForm['kind']) => {
    const partner = emptyPartner(kind)
    const index = value.partners.length
    flushSync(() => set({ partners: [...value.partners, partner] }))
    document.getElementById(fieldId(`applicant.partners[${index}].${kind === 'INDIVIDUAL' ? 'firstName' : 'name'}`))?.focus()
    announce(`Dodano partnera ${index + 1}. Liczba partnerów: ${index + 1} z ${MAX_PARTNERS}.`)
  }

  const removePartner = (index: number) => {
    flushSync(() => set({ partners: value.partners.filter((_, i) => i !== index) }))
    document.getElementById(fieldId('applicant.partners'))?.focus()
    announce(`Usunięto partnera ${index + 1}. Liczba partnerów: ${value.partners.length - 1} z ${MAX_PARTNERS}.`)
  }

  return (
    <>
      <Section legend="Partnerzy grupy">
        <Text id={fieldId('applicant.partners')} tabIndex={-1} style={{ outline: 'none' }}>
          Dodaj od 1 do {MAX_PARTNERS} partnerów – każdy może być osobą lub podmiotem. Liczba partnerów:{' '}
          <strong>
            {value.partners.length} z {MAX_PARTNERS}
          </strong>
          .
        </Text>
        {partnersError && (
          <Text c={ERROR_TEXT_COLOR} fw={500} role="alert">
            {partnersError}
          </Text>
        )}
        {value.partners.map((partner, index) => (
          <Fieldset
            key={partner.id}
            legend={partnerLabel(index, partner)}
            styles={{ legend: { fontSize: '1.05rem', fontWeight: 700 } }}
          >
            <Stack gap="sm">
              <PartnerFields
                index={index}
                value={partner}
                onChange={(next) => set({ partners: value.partners.map((p, i) => (i === index ? next : p)) })}
              />
              <Group>
                <Button
                  variant="outline"
                  color="red"
                  leftSection={<IconTrash size={18} aria-hidden="true" />}
                  onClick={() => removePartner(index)}
                  disabled={value.partners.length <= 1}
                >
                  Usuń partnera {index + 1}
                </Button>
                {value.partners.length <= 1 && (
                  <Text size="sm" c="dimmed">
                    Grupa musi mieć co najmniej jednego partnera.
                  </Text>
                )}
              </Group>
            </Stack>
          </Fieldset>
        ))}
        {value.partners.length < MAX_PARTNERS ? (
          <Group>
            <Button
              variant="light"
              leftSection={<IconPlus size={18} aria-hidden="true" />}
              onClick={() => addPartner('INDIVIDUAL')}
            >
              Dodaj partnera: osobę
            </Button>
            <Button
              variant="light"
              leftSection={<IconPlus size={18} aria-hidden="true" />}
              onClick={() => addPartner('ENTITY')}
            >
              Dodaj partnera: podmiot
            </Button>
          </Group>
        ) : (
          <Alert icon={<IconInfoCircle size={20} aria-hidden="true" />} color="blue" variant="light" role="note">
            Osiągnięto limit {MAX_PARTNERS} partnerów. Aby dodać kolejnego, usuń jednego z obecnych.
          </Alert>
        )}
      </Section>
      <Section legend="Reprezentant lub reprezentantka grupy">
        <TextField
          path="applicant.representative.firstName"
          label="Imię"
          value={value.representative.firstName}
          onChange={(firstName) => setRepresentative({ firstName })}
        />
        <TextField
          path="applicant.representative.lastName"
          label="Nazwisko"
          value={value.representative.lastName}
          onChange={(lastName) => setRepresentative({ lastName })}
        />
        <TextField
          path="applicant.representative.phone"
          label="Telefon"
          value={value.representative.phone}
          onChange={(phone) => setRepresentative({ phone })}
          type="tel"
          inputMode="tel"
        />
        <TextField
          path="applicant.representative.email"
          label="Adres e-mail"
          value={value.representative.email}
          onChange={(email) => setRepresentative({ email })}
          type="email"
          inputMode="email"
        />
      </Section>
    </>
  )
}
