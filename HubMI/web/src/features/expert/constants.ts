export type ExpertTab = 'pomysly' | 'konsultacje' | 'doradztwo'

export const EXPERT_TAB_LABELS: Record<ExpertTab, string> = {
  pomysly: 'Pomysły do zaopiniowania',
  konsultacje: 'Moje konsultacje i wątki',
  doradztwo: 'Doradztwo dla samorządów (JST)',
}

export const IDEA_STAGE_LABELS: Record<string, string> = {
  IDEA: 'Koncepcja / Fiszka',
  PROTOTYPE: 'Prototypowanie',
  PILOT: 'Pilotaż / Testy',
  SCALING: 'Skalowanie / Wdrożenie',
}

export const IDEA_STATUS_CONFIG: Record<string, { label: string; color: string; description: string }> = {
  SUBMITTED: {
    label: 'Zgłoszony (nowy)',
    color: 'yellow',
    description: 'Nowy pomysł oczekujący na wstępną analizę merytoryczną i formalną',
  },
  IN_REVIEW: {
    label: 'W trakcie weryfikacji',
    color: 'blue',
    description: 'Trwa analiza merytoryczna przez ekspertów i pracowników ROPS',
  },
  ACCEPTED: {
    label: 'Zaakceptowany',
    color: 'green',
    description: 'Pomysł zakwalifikowany do etapu wsparcia i rozwoju',
  },
  REJECTED: {
    label: 'Odrzucony',
    color: 'red',
    description: 'Pomysł nie spełnia kryteriów innowacyjności lub formalnych',
  },
}
