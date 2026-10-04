export const IDEA_STATUS_CONFIG: Record<
  string,
  { label: string; color: string; description: string }
> = {
  SUBMITTED: {
    label: 'Zgłoszony (Nowy)',
    color: 'blue',
    description: 'Nowy pomysł oczekujący na wstępną weryfikację formalną ROPS',
  },
  IN_REVIEW: {
    label: 'W trakcie weryfikacji',
    color: 'yellow',
    description: 'Pomysł jest analizowany przez ekspertów ROPS',
  },
  ACCEPTED: {
    label: 'Zaakceptowany',
    color: 'teal',
    description: 'Pomysł zakwalifikowany do etapu inkubacji lub testów',
  },
  REJECTED: {
    label: 'Odrzucony',
    color: 'red',
    description: 'Pomysł nie spełnia kryteriów lub wymaga ponownego opracowania',
  },
  DRAFT: {
    label: 'Szkic',
    color: 'gray',
    description: 'Wersja robocza zapisana przez autora',
  },
}

export const TEST_REQUEST_STATUS_CONFIG: Record<
  string,
  { label: string; color: string; description: string }
> = {
  NEW: {
    label: 'Nowe zgłoszenie',
    color: 'blue',
    description: 'Oczekuje na weryfikację przez koordynatora ROPS',
  },
  ACCEPTED: {
    label: 'Zaakceptowane',
    color: 'teal',
    description: 'Zgłoszenie zatwierdzone, tester zaproszony do udziału',
  },
  DECLINED: {
    label: 'Odrzucone',
    color: 'red',
    description: 'Zgłoszenie nie mogło zostać zakwalifikowane',
  },
}

export const MATERIAL_TYPE_LABELS: Record<string, string> = {
  GUIDE: 'Poradnik krok po kroku',
  DOCUMENT: 'Dokument i raport',
  TOOLKIT: 'Narzędziownik / Zestaw ćwiczeń',
  VIDEO: 'Materiał wideo (z transkrypcją WCAG)',
  CANVAS: 'Canva innowacji społecznej',
  OTHER: 'Inny materiał edukacyjny',
}
