import { BUDGET_RANGE, formatPln, MAX_CONTEXT_LENGTH, STAFF_RANGE } from './constants'

export interface InstitutionValues {
  type: string | null
  staffCount: number | string
  budgetPln: number | string
  context: string
}

export interface Institution {
  type: string
  staffCount: number
  budgetPln: number
  context: string
}

export type InstitutionErrors = Partial<Record<'type' | 'staffCount' | 'budgetPln' | 'context', string>>

function wholeNumber(value: number | string): number | null {
  const parsed = typeof value === 'number' ? value : Number(value)
  return value !== '' && Number.isInteger(parsed) ? parsed : null
}

/** The same ranges as the server's `InstitutionDraft`. */
export function validateInstitution(values: InstitutionValues): {
  errors: InstitutionErrors
  institution: Institution | null
} {
  const staffCount = wholeNumber(values.staffCount)
  const budgetPln = wholeNumber(values.budgetPln)
  const context = values.context.trim()
  const errors: InstitutionErrors = {
    ...(values.type === null ? { type: 'Wybierz rodzaj instytucji.' } : {}),
    ...(staffCount === null || staffCount < STAFF_RANGE.min || staffCount > STAFF_RANGE.max
      ? { staffCount: `Wpisz liczbę osób od ${STAFF_RANGE.min} do ${STAFF_RANGE.max}.` }
      : {}),
    ...(budgetPln === null || budgetPln < BUDGET_RANGE.min || budgetPln > BUDGET_RANGE.max
      ? { budgetPln: `Wpisz budżet od ${formatPln(BUDGET_RANGE.min)} do ${formatPln(BUDGET_RANGE.max)}.` }
      : {}),
    ...(context === '' ? { context: 'Opisz w kilku zdaniach warunki w Twojej instytucji.' } : {}),
    ...(context.length > MAX_CONTEXT_LENGTH ? { context: `Opis może mieć najwyżej ${MAX_CONTEXT_LENGTH} znaków.` } : {}),
  }
  if (Object.keys(errors).length > 0 || values.type === null || staffCount === null || budgetPln === null) {
    return { errors, institution: null }
  }
  return { errors, institution: { type: values.type, staffCount, budgetPln, context } }
}
