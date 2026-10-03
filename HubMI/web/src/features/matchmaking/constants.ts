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
  AGING: 'Wsparcie seniorów i osób starszych',
  MENTAL_HEALTH: 'Zdrowie psychiczne i samopoczucie',
  LONELINESS: 'Przeciwdziałanie samotności i integracja',
  DIGITAL_EXCLUSION: 'Pomoc w korzystaniu z internetu i technologii',
  SERVICE_ACCESS: 'Łatwiejszy dojazd do lekarza i urzędu',
  COORDINATION: 'Współpraca lokalna i pomoc sąsiedzka',
  DEPOPULATION: 'Wsparcie małych miejscowości i wsi',
  OTHER: 'Inne potrzeby społeczne',
}

export const INNOVATION_STAGE_NAMES: Record<string, string> = {
  IDEA: 'Nowy pomysł (w przygotowaniu)',
  PILOT: 'Sprawdzane w praktyce (pilotaż)',
  TESTED: 'Przetestowane z mieszkańcami',
  IMPLEMENTED: 'Gotowe i działające rozwiązanie',
}

export function getMatchQualityLabel(score: number): {
  label: string
  color: 'teal' | 'blue' | 'yellow'
} {
  if (score >= 0.75) {
    return { label: 'Bardzo wysoka zgodność z problemem', color: 'teal' }
  }
  if (score >= 0.45) {
    return { label: 'Dobra zgodność z problemem', color: 'blue' }
  }
  return { label: 'Częściowa zgodność (warto sprawdzić)', color: 'yellow' }
}
