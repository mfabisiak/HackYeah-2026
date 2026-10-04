export type StepId = 'title' | 'applicant' | 'description' | 'diagnosis' | 'audience' | 'plan' | 'budget' | 'summary'

export interface StepDefinition {
  id: StepId
  title: string
  /** Point(s) of the ROPS form the step corresponds to. */
  points: string
}

export const STEPS: readonly StepDefinition[] = [
  { id: 'title', title: 'Tytuł innowacji', points: 'pkt 1' },
  { id: 'applicant', title: 'Wnioskodawca', points: 'pkt 2' },
  { id: 'description', title: 'Opis i innowacyjność', points: 'pkt 3–4' },
  { id: 'diagnosis', title: 'Diagnoza problemu', points: 'pkt 5' },
  { id: 'audience', title: 'Odbiorcy i zmiana', points: 'pkt 6–8' },
  { id: 'plan', title: 'Plan działania i koszty', points: 'pkt 9' },
  { id: 'budget', title: 'Kwota grantu i zespół', points: 'pkt 10–11' },
  { id: 'summary', title: 'Oświadczenia i złożenie', points: 'pkt 12' },
]

const FIELD_STEPS: readonly (readonly [string, StepId])[] = [
  ['title', 'title'],
  ['applicant', 'applicant'],
  ['description', 'description'],
  ['innovativeness', 'description'],
  ['problemDiagnosis', 'diagnosis'],
  ['socialArea', 'diagnosis'],
  ['audienceDescription', 'audience'],
  ['expectedChange', 'audience'],
  ['futureVision', 'audience'],
  ['plan', 'plan'],
  ['requestedGrantAmountGrosze', 'budget'],
  ['projectTeam', 'budget'],
  ['declarations', 'summary'],
]

/** The step that owns a field path such as `plan.preparation[1].costGrosze`; `undefined` for unknown fields. */
export function stepForField(path: string): StepId | undefined {
  const root = path.split(/[.[]/)[0]
  return FIELD_STEPS.find(([name]) => name === root)?.[1]
}

/** DOM id of the input for a field path (`plan.preparation[1].term` becomes `f-plan-preparation-1-term`). */
export function fieldId(path: string): string {
  return `f-${path.replace(/\[(\d+)\]/g, '-$1').replace(/\./g, '-')}`
}

/** Paths the server reports for individual plan items (`plan.items[3].costGrosze`) have no single input to point at. */
export function fieldIdOrStep(path: string): string {
  return fieldId(path.replace(/^plan\.items\[\d+]\..*$/, 'plan'))
}

export const PHASE_TITLES = {
  preparation: 'Okres przygotowawczy',
  testingPhase1: 'Testowanie – Faza I',
  testingPhase2: 'Testowanie – Faza II',
} as const

const stepTitle = (id: StepId): string => STEPS.find((s) => s.id === id)?.title ?? ''

/** Where a field lives, for the error summary (“Testowanie – Faza I, pozycja 2”). */
export function fieldContext(path: string): string {
  const partner = /^applicant\.partners\[(\d+)]/.exec(path)
  if (partner) return `Wnioskodawca, partner ${Number(partner[1]) + 1}`
  if (path.startsWith('applicant.representative')) return 'Wnioskodawca, osoba reprezentująca'
  if (path.startsWith('applicant.contactPerson')) return 'Wnioskodawca, osoba do kontaktów roboczych'
  if (path.startsWith('applicant.address')) return 'Wnioskodawca, adres'
  const planRow = /^plan\.(preparation|testingPhase1|testingPhase2)\[(\d+)]/.exec(path)
  if (planRow) return `${PHASE_TITLES[planRow[1] as keyof typeof PHASE_TITLES]}, pozycja ${Number(planRow[2]) + 1}`
  const phase = /^plan\.(preparation|testingPhase1|testingPhase2)$/.exec(path)
  if (phase) return PHASE_TITLES[phase[1] as keyof typeof PHASE_TITLES]
  const step = stepForField(path)
  return step ? stepTitle(step) : 'Wniosek'
}
