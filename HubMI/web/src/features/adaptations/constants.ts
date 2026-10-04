export const INSTITUTION_TYPE_OPTIONS = [
  { value: 'LOCAL_GOVERNMENT', label: 'Samorząd (gmina, powiat)' },
  { value: 'NGO', label: 'Organizacja pozarządowa' },
  { value: 'SOCIAL_SERVICES_CENTER', label: 'Centrum usług społecznych' },
  { value: 'OTHER', label: 'Inna instytucja' },
] as const

export const INSTITUTION_TYPE_NAMES: Record<string, string> = Object.fromEntries(
  INSTITUTION_TYPE_OPTIONS.map((option) => [option.value, option.label]),
)

export const ADAPTATION_STATUS: Record<string, { label: string; color: string }> = {
  PENDING_REVIEW: { label: 'Czeka na przegląd ROPS', color: 'blue' },
  APPROVED: { label: 'Zatwierdzony przez ROPS', color: 'teal' },
  REJECTED: { label: 'Odrzucony przez ROPS', color: 'red' },
}

export const ADAPTATION_STATUS_FILTERS = [
  { value: 'PENDING_REVIEW', label: 'Do przeglądu' },
  { value: 'APPROVED', label: 'Zatwierdzone' },
  { value: 'REJECTED', label: 'Odrzucone' },
] as const

/** What the API reports when the model could not write a plan; nothing was stored then. */
export const ADAPTATION_AI_NOTICES: Record<string, string> = {
  UNAVAILABLE:
    'Asystent AI jest chwilowo niedostępny albo zajęty innymi pytaniami, więc plan nie powstał. Spróbuj ponownie za chwilę.',
  INVALID_OUTPUT:
    'Asystent AI nie przygotował kompletnego planu, więc nic nie zapisaliśmy. Spróbuj ponownie lub opisz instytucję nieco inaczej.',
}

export const STAFF_RANGE = { min: 0, max: 10_000 } as const
export const BUDGET_RANGE = { min: 1_000, max: 50_000_000 } as const
export const MAX_CONTEXT_LENGTH = 600

export function formatPln(amount: number): string {
  return `${new Intl.NumberFormat('pl-PL').format(amount)} zł`
}

/** The pilot's months are numbered from 0 in the API and from 1 for people. */
export function monthLabel(startMonth: number): string {
  return `Miesiąc ${startMonth + 1}`
}
