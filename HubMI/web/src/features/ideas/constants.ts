export { TARGET_GROUP_NAMES, TARGET_GROUP_OPTIONS, INNOVATION_STAGE_NAMES } from '../knowledge/constants'

export const MAX_IDEA_TITLE_LENGTH = 200
export const MAX_IDEA_ESSENCE_LENGTH = 1000

export const IDEA_STAGE_OPTIONS = [
  { value: 'IDEA', label: 'Nowy pomysł (w przygotowaniu)', hint: 'Mam pomysł, ale jeszcze go nie sprawdzałem(-am) w praktyce.' },
  { value: 'PILOT', label: 'Sprawdzane w praktyce (pilotaż)', hint: 'Wypróbowuję rozwiązanie na małą skalę.' },
  { value: 'TESTED', label: 'Przetestowane z mieszkańcami', hint: 'Rozwiązanie było już testowane z odbiorcami.' },
  { value: 'IMPLEMENTED', label: 'Gotowe i działające rozwiązanie', hint: 'Rozwiązanie już działa i można je powielać.' },
] as const

export const IDEA_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Szkic',
  SUBMITTED: 'Zgłoszony – czeka na przegląd',
  IN_REVIEW: 'W ocenie ROPS',
  ACCEPTED: 'Przyjęty',
  REJECTED: 'Nieprzyjęty',
}

export const IDEA_STATUS_DESCRIPTIONS: Record<string, string> = {
  DRAFT: 'Pomysł nie został jeszcze zgłoszony do ROPS.',
  SUBMITTED: 'Pomysł dotarł do ROPS i czeka na przegląd. Nie musisz nic robić.',
  IN_REVIEW: 'Pracownicy ROPS oceniają Twój pomysł. Damy znać, gdy będzie decyzja.',
  ACCEPTED: 'Pomysł został przyjęty. Możesz przygotować na jego podstawie wniosek o mikrogrant.',
  REJECTED: 'Pomysł nie został przyjęty. Uzasadnienie znajdziesz poniżej.',
}
