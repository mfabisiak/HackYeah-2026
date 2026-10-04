import type { ReactNode } from 'react'
import { Button, Group, Paper, Stack, Text, Title } from '@mantine/core'
import { IconPencil } from '@tabler/icons-react'
import { SOCIAL_AREA_NAMES } from '../knowledge/constants'
import { APPLICANT_TYPE_LABELS } from './ApplicantStep'
import {
  PLAN_PHASES,
  formatGrosze,
  formatTerm,
  parsePln,
  phaseTotalGrosze,
  planTotalGrosze,
  rowCostGrosze,
  rowTerm,
  type AddressForm,
  type ApplicationForm,
  type PartnerForm,
} from './form'
import { PHASE_TITLES, STEPS, type StepId } from './steps'

type Entry = readonly [label: string, value: string]

const formatAddress = (a: AddressForm): string =>
  [
    `${a.street} ${a.buildingNumber}${a.apartmentNumber.trim() ? `/${a.apartmentNumber.trim()}` : ''}`,
    `${a.postalCode} ${a.city}`,
  ].join(', ')

function partnerEntries(index: number, p: PartnerForm): Entry[] {
  const who = `Partner ${index + 1}`
  return p.kind === 'INDIVIDUAL'
    ? [
        [who, `${p.firstName} ${p.lastName}`],
        [`${who}: adres`, formatAddress(p.address)],
        [`${who}: kontakt`, `${p.phone}, ${p.email}`],
      ]
    : [
        [who, p.name],
        [`${who}: KRS / REGON / NIP`, `${p.krs} / ${p.regon} / ${p.nip}`],
        [`${who}: adres`, formatAddress(p.address)],
        [`${who}: kontakt`, `${p.phone}, ${p.email}`],
      ]
}

function applicantEntries(a: ApplicationForm['applicant']): Entry[] {
  if (!a) return []
  const type: Entry = ['Rodzaj wnioskodawcy', APPLICANT_TYPE_LABELS[a.type]]
  switch (a.type) {
    case 'INDIVIDUAL':
      return [
        type,
        ['Imię i nazwisko', `${a.firstName} ${a.lastName}`],
        ['Adres', formatAddress(a.address)],
        ['Kontakt', `${a.phone}, ${a.email}`],
      ]
    case 'ENTITY':
      return [
        type,
        ['Nazwa podmiotu', a.name],
        ['KRS / REGON / NIP', `${a.krs} / ${a.regon} / ${a.nip}`],
        ['Adres siedziby', formatAddress(a.address)],
        ['Kontakt do podmiotu', `${a.phone}, ${a.email}`],
        [
          'Osoba reprezentująca',
          `${a.representative.fullName}, ${a.representative.function}, ${a.representative.phone}, ${a.representative.email}`,
        ],
        [
          'Osoba do kontaktów roboczych',
          `${a.contactPerson.fullName}, ${a.contactPerson.function}, ${a.contactPerson.phone}, ${a.contactPerson.email}`,
        ],
      ]
    case 'NON_FORMAL_GROUP':
      return [
        type,
        ...a.partners.flatMap((p, i) => partnerEntries(i, p)),
        [
          'Reprezentant grupy',
          `${a.representative.firstName} ${a.representative.lastName}, ${a.representative.phone}, ${a.representative.email}`,
        ],
      ]
  }
}

function Definition({ entries }: { entries: readonly Entry[] }) {
  return (
    <Stack component="dl" gap="sm" m={0}>
      {entries.map(([label, value]) => (
        <div key={label}>
          <Text component="dt" fw={700}>
            {label}
          </Text>
          <Text component="dd" m={0} style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
            {value.trim() || <i>nie wypełniono</i>}
          </Text>
        </div>
      ))}
    </Stack>
  )
}

function PlanSummary({ plan }: { plan: ApplicationForm['plan'] }) {
  return (
    <Stack gap="md">
      {PLAN_PHASES.map((phase) => (
        <div key={phase}>
          <Text fw={700}>
            {PHASE_TITLES[phase]} – suma {formatGrosze(phaseTotalGrosze(plan[phase]))}
          </Text>
          {plan[phase].length === 0 ? (
            <Text fs="italic">brak pozycji</Text>
          ) : (
            <Stack component="ul" gap={4} m={0} pl="md">
              {plan[phase].map((row) => (
                <li key={row.id}>
                  {row.action || 'bez opisu'} – {rowTerm(row) ? formatTerm(rowTerm(row)) : 'bez terminu'} –{' '}
                  {formatGrosze(rowCostGrosze(row))}
                </li>
              ))}
            </Stack>
          )}
        </div>
      ))}
      <Text fw={700}>Suma kosztów ogółem: {formatGrosze(planTotalGrosze(plan))}</Text>
    </Stack>
  )
}

function SummarySection({
  step,
  onEdit,
  children,
}: {
  step: StepId
  onEdit?: (step: StepId) => void
  children: ReactNode
}) {
  const definition = STEPS.find((s) => s.id === step)
  const id = `summary-${step}`
  return (
    <Paper component="section" withBorder p="md" radius="md" aria-labelledby={id}>
      <Group justify="space-between" align="flex-start" mb="sm" gap="sm">
        <Title order={3} size="h4" id={id}>
          {definition?.title}
        </Title>
        {onEdit && (
          <Button
            variant="subtle"
            leftSection={<IconPencil size={18} aria-hidden="true" />}
            onClick={() => onEdit(step)}
            aria-label={`Zmień: ${definition?.title}`}
          >
            Zmień
          </Button>
        )}
      </Group>
      {children}
    </Paper>
  )
}

interface ApplicationSummaryProps {
  form: ApplicationForm
  /** When given, every section has a “Zmień” button leading back to its step. */
  onEdit?: (step: StepId) => void
}

export function ApplicationSummary({ form, onEdit }: ApplicationSummaryProps) {
  const grant = parsePln(form.requestedGrant)
  return (
    <Stack gap="md">
      <SummarySection step="title" onEdit={onEdit}>
        <Definition entries={[['Tytuł innowacji', form.title]]} />
      </SummarySection>
      <SummarySection step="applicant" onEdit={onEdit}>
        {form.applicant ? (
          <Definition entries={applicantEntries(form.applicant)} />
        ) : (
          <Text fs="italic">Nie wybrano rodzaju wnioskodawcy.</Text>
        )}
      </SummarySection>
      <SummarySection step="description" onEdit={onEdit}>
        <Definition
          entries={[
            ['Opis innowacji', form.description],
            ['Innowacyjność rozwiązania', form.innovativeness],
          ]}
        />
      </SummarySection>
      <SummarySection step="diagnosis" onEdit={onEdit}>
        <Definition
          entries={[
            ['Obszar z Mapy Wyzwań Społecznych', SOCIAL_AREA_NAMES[form.socialArea] ?? ''],
            ['Diagnoza problemu', form.problemDiagnosis],
          ]}
        />
      </SummarySection>
      <SummarySection step="audience" onEdit={onEdit}>
        <Definition
          entries={[
            ['Odbiorcy innowacji', form.audienceDescription],
            ['Zmiana, jaką wprowadza innowacja', form.expectedChange],
            ['Wizja przyszłości', form.futureVision],
          ]}
        />
      </SummarySection>
      <SummarySection step="plan" onEdit={onEdit}>
        <PlanSummary plan={form.plan} />
      </SummarySection>
      <SummarySection step="budget" onEdit={onEdit}>
        <Definition
          entries={[
            ['Wnioskowana kwota grantu', grant === null ? '' : formatGrosze(grant)],
            ['Zespół projektowy', form.projectTeam],
          ]}
        />
      </SummarySection>
    </Stack>
  )
}
