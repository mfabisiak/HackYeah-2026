export const MALOPOLSKA_MUNICIPALITIES = [
  'Kraków',
  'Tarnów',
  'Nowy Sącz',
  'Oświęcim',
  'Chrzanów',
  'Olkusz',
  'Nowy Targ',
  'Bochnia',
  'Gorlice',
  'Zakopane',
  'Skawina',
  'Wieliczka',
  'Andrychów',
  'Trzebinia',
  'Wadowice',
  'Kęty',
  'Myślenice',
  'Libiąż',
  'Brzesko',
  'Limanowa',
  'Rabka-Zdrój',
  'Dąbrowa Tarnowska',
  'Miechów',
  'Niepołomice',
  'Krynica-Zdrój',
] as const

export const SAMPLE_QUERIES = [
  {
    label: 'Transport seniorów',
    text: 'Brak transportu dla seniorów i osób niesamodzielnych na wizyty lekarskie i rehabilitację w małych miejscowościach.',
  },
  {
    label: 'Dostępność urzędu',
    text: 'Trudności osób niesłyszących oraz niedowidzących w załatwianiu spraw urzędowych i brak tłumacza języka migowego.',
  },
  {
    label: 'Integracja i samotność',
    text: 'Samotność osób starszych po stracie współmałżonka i brak miejsc codziennych spotkań integrujących pokolenia.',
  },
] as const

export const SOCIAL_AREA_NAMES: Record<string, string> = {
  AGING: 'Starzenie się społeczeństwa',
  MENTAL_HEALTH: 'Zdrowie psychiczne',
  LONELINESS: 'Samotność i relacje',
  DIGITAL_EXCLUSION: 'Wykluczenie cyfrowe',
  SERVICE_ACCESS: 'Dostęp do usług publicznych',
  COORDINATION: 'Koordynacja wsparcia społecznego',
  DEPOPULATION: 'Depopulacja i migracje',
  OTHER: 'Inne wyzwania społeczne',
}

export const INNOVATION_STAGE_NAMES: Record<string, string> = {
  IDEA: 'Pomysł',
  PILOT: 'W trakcie pilotażu',
  TESTED: 'Sprawdzony prototyp',
  IMPLEMENTED: 'Wdrożone rozwiązanie',
}

export function getMatchQualityLabel(score: number): {
  label: string
  color: 'teal' | 'blue' | 'yellow'
} {
  if (score >= 0.75) {
    return { label: 'Bardzo wysokie dopasowanie', color: 'teal' }
  }
  if (score >= 0.45) {
    return { label: 'Dobre dopasowanie', color: 'blue' }
  }
  return { label: 'Możliwe dopasowanie', color: 'yellow' }
}
