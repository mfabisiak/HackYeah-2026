import { flushSync } from 'react-dom'
import { Button, Fieldset, Group, NativeSelect, Paper, Stack, Text, Title, VisuallyHidden } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconPlus, IconTrash } from '@tabler/icons-react'
import { ERROR_TEXT_COLOR, INPUT_STYLES } from '../../components/formStyles'
import { TextField, useErrorLookup, useFieldError } from './fields'
import { useAnnouncer } from './announcer'
import {
  MAX_PLAN_ROWS,
  MAX_PREPARATION_MONTHS,
  MAX_TESTING_MONTHS,
  MONTH_NAMES,
  PLAN_PHASES,
  emptyPlanRow,
  formatGrosze,
  phaseTotalGrosze,
  planDurationMonths,
  planTotalGrosze,
  type PlanForm,
  type PlanPhase,
  type PlanRowForm,
} from './form'
import { PHASE_TITLES, fieldId } from './steps'

const PHASE_HINTS: Record<PlanPhase, string> = {
  preparation: `Przygotowanie innowacji do testów, najwyżej ${MAX_PREPARATION_MONTHS} miesiące. Dodaj co najmniej jedno zadanie.`,
  testingPhase1: 'Pierwszy etap testowania innowacji z odbiorcami. Dodaj co najmniej jedno zadanie.',
  testingPhase2: `Drugi etap testowania (nieobowiązkowy). Fazy I i II razem mogą trwać najwyżej ${MAX_TESTING_MONTHS} miesięcy.`,
}

const MONTH_OPTIONS = [
  { value: '', label: 'Wybierz miesiąc' },
  ...MONTH_NAMES.map((name, i) => ({ value: String(i + 1).padStart(2, '0'), label: name })),
]

function yearOptions(current: string): { value: string; label: string }[] {
  const thisYear = new Date().getFullYear()
  const years = Array.from({ length: 5 }, (_, i) => String(thisYear - 1 + i))
  const all = current && !years.includes(current) ? [...years, current].sort() : years
  return [{ value: '', label: 'Wybierz rok' }, ...all.map((y) => ({ value: y, label: y }))]
}

function Duration({ label, months, max }: { label: string; months: number; max: number }) {
  const over = months > max
  return (
    <Text size="md" c={over ? ERROR_TEXT_COLOR : undefined} fw={over ? 700 : 500}>
      {label}: {months} z {max} miesięcy{over ? ' – przekroczono limit' : ''}
    </Text>
  )
}

interface RowEditorProps {
  phase: PlanPhase
  index: number
  row: PlanRowForm
  onChange: (row: PlanRowForm) => void
  onRemove: () => void
}

function RowEditor({ phase, index, row, onChange, onRemove }: RowEditorProps) {
  const prefix = `plan.${phase}[${index}]`
  const termError = useFieldError(`${prefix}.term`)
  const name = row.action.trim()
  const shortName = name.length > 40 ? `${name.slice(0, 40)}…` : name
  return (
    <Fieldset
      legend={`Pozycja ${index + 1}${shortName ? `: ${shortName}` : ''}`}
      styles={{ legend: { fontSize: '1.05rem', fontWeight: 700 } }}
    >
      <Stack gap="sm">
        <TextField
          path={`${prefix}.action`}
          label="Opis zadania"
          value={row.action}
          onChange={(action) => onChange({ ...row, action })}
        />
        <Group grow align="flex-start">
          <NativeSelect
            id={fieldId(`${prefix}.term`)}
            label="Miesiąc"
            data={MONTH_OPTIONS}
            value={row.month}
            onChange={(event) => onChange({ ...row, month: event.currentTarget.value })}
            error={termError}
            required
            size="md"
            styles={INPUT_STYLES}
          />
          <NativeSelect
            label="Rok"
            data={yearOptions(row.year)}
            value={row.year}
            onChange={(event) => onChange({ ...row, year: event.currentTarget.value })}
            error={termError ? true : undefined}
            required
            size="md"
            styles={INPUT_STYLES}
          />
        </Group>
        <TextField
          path={`${prefix}.costGrosze`}
          label="Koszt (zł)"
          description="Kwota w złotych, np. 1200 lub 1200,50"
          value={row.cost}
          onChange={(cost) => onChange({ ...row, cost })}
          inputMode="decimal"
          autoComplete="off"
        />
        <Group>
          <Button
            variant="outline"
            color="red"
            leftSection={<IconTrash size={18} aria-hidden="true" />}
            onClick={onRemove}
          >
            Usuń pozycję {index + 1}
            {shortName ? `: ${shortName}` : ''}
          </Button>
        </Group>
      </Stack>
    </Fieldset>
  )
}

interface PlanStepProps {
  plan: PlanForm
  onChange: (plan: PlanForm) => void
}

export function PlanStep({ plan, onChange }: PlanStepProps) {
  const announce = useAnnouncer()
  const errorFor = useErrorLookup()
  const total = planTotalGrosze(plan)
  // Sums change on every keystroke; announce them once typing pauses so screen readers are not flooded.
  const [announcedTotal] = useDebouncedValue(total, 900)

  const setRows = (phase: PlanPhase, rows: PlanRowForm[]) => onChange({ ...plan, [phase]: rows })

  const addRow = (phase: PlanPhase) => {
    const index = plan[phase].length
    flushSync(() => setRows(phase, [...plan[phase], emptyPlanRow()]))
    document.getElementById(fieldId(`plan.${phase}[${index}].action`))?.focus()
    announce(`Dodano pozycję ${index + 1}: ${PHASE_TITLES[phase]}.`)
  }

  const removeRow = (phase: PlanPhase, index: number) => {
    const remaining = plan[phase].filter((_, i) => i !== index)
    flushSync(() => setRows(phase, remaining))
    const neighbour = Math.min(index, remaining.length - 1)
    const target =
      neighbour >= 0 ? fieldId(`plan.${phase}[${neighbour}].action`) : fieldId(`plan.${phase}`)
    document.getElementById(target)?.focus()
    announce(`Usunięto pozycję ${index + 1}: ${PHASE_TITLES[phase]}. Pozostało pozycji: ${remaining.length}.`)
  }

  return (
    <Stack gap="xl">
      <Paper withBorder p="md" radius="md">
        <Stack gap={4}>
          <Title order={3} size="h5">
            Limity czasu
          </Title>
          <Duration
            label="Okres przygotowawczy"
            months={planDurationMonths(plan.preparation)}
            max={MAX_PREPARATION_MONTHS}
          />
          <Duration
            label="Testowanie (Faza I i II razem)"
            months={planDurationMonths([...plan.testingPhase1, ...plan.testingPhase2])}
            max={MAX_TESTING_MONTHS}
          />
          <Text size="sm" c="dimmed">
            Liczymy miesiące od pierwszego do ostatniego zadania w danym okresie, łącznie z tymi miesiącami.
          </Text>
        </Stack>
      </Paper>

      {PLAN_PHASES.map((phase) => {
        const rows = plan[phase]
        const groupError = errorFor(`plan.${phase}`)
        return (
          <section key={phase} aria-labelledby={`${phase}-heading`}>
            <Stack gap="sm">
              <Title order={3} size="h4" id={`${phase}-heading`}>
                {PHASE_TITLES[phase]}
              </Title>
              <Text c="dimmed">{PHASE_HINTS[phase]}</Text>
              <div id={fieldId(`plan.${phase}`)} tabIndex={-1} style={{ outline: 'none' }}>
                {groupError && (
                  <Text c={ERROR_TEXT_COLOR} fw={500} role="alert">
                    {groupError}
                  </Text>
                )}
              </div>
              {rows.map((row, index) => (
                <RowEditor
                  key={row.id}
                  phase={phase}
                  index={index}
                  row={row}
                  onChange={(next) => setRows(phase, rows.map((r, i) => (i === index ? next : r)))}
                  onRemove={() => removeRow(phase, index)}
                />
              ))}
              {rows.length === 0 && <Text fs="italic">Nie dodano jeszcze żadnej pozycji.</Text>}
              <Group justify="space-between" align="center">
                {rows.length < MAX_PLAN_ROWS ? (
                  <Button
                    variant="light"
                    leftSection={<IconPlus size={18} aria-hidden="true" />}
                    onClick={() => addRow(phase)}
                  >
                    Dodaj pozycję: {PHASE_TITLES[phase].toLowerCase()}
                  </Button>
                ) : (
                  <Text>Osiągnięto limit {MAX_PLAN_ROWS} pozycji w tej części.</Text>
                )}
                <Text fw={600}>Suma: {formatGrosze(phaseTotalGrosze(rows))}</Text>
              </Group>
            </Stack>
          </section>
        )
      })}

      <Paper withBorder p="md" radius="md">
        <Text fw={700} size="lg">
          Suma kosztów ogółem: {formatGrosze(total)}
        </Text>
      </Paper>
      <VisuallyHidden role="status">Suma kosztów ogółem: {formatGrosze(announcedTotal)}</VisuallyHidden>
    </Stack>
  )
}
