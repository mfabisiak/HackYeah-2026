export const ASSIST_MODES = [
  {
    value: 'SIMILAR',
    label: 'Sprawdź, czy to już istnieje',
    description: 'Znajdziemy podobne rozwiązania w bazie innowacji.',
  },
  {
    value: 'EXPAND',
    label: 'Podpowiedz, jak rozwinąć pomysł',
    description: 'Asystent zaproponuje trzy kierunki rozwoju i pierwszy krok.',
  },
  {
    value: 'RISKS',
    label: 'Pokaż, co może pójść nie tak',
    description: 'Asystent wskaże ryzyka i podpowie, jak je sprawdzić przed startem.',
  },
  {
    value: 'FLOW',
    label: 'Pokaż, jak to ma działać krok po kroku',
    description: 'Asystent rozpisze, kto z kim współpracuje i w jakiej kolejności.',
  },
] as const

export const ASSIST_MODE_NAMES: Record<string, string> = Object.fromEntries(
  ASSIST_MODES.map((mode) => [mode.value, mode.label]),
)

export const NOVELTY_NOTICES: Record<string, { color: string; title: string; text: string }> = {
  ALREADY_EXISTS: {
    color: 'orange',
    title: 'Takie rozwiązanie już działa',
    text: 'W bazie jest bardzo podobna innowacja. Zobacz, czy możesz ją dostosować do swojej instytucji zamiast zaczynać od zera.',
  },
  PARTIAL: {
    color: 'blue',
    title: 'Coś podobnego już jest',
    text: 'W bazie są powiązane rozwiązania. Twój pomysł może je uzupełnić. Sprawdź, czym się różni.',
  },
  NEW: {
    color: 'teal',
    title: 'Nie znaleźliśmy podobnego rozwiązania',
    text: 'W bazie nie ma jeszcze nic zbliżonego. Twój pomysł może wypełniać lukę, o której warto powiedzieć ROPS.',
  },
}

/** What the API reports when the model could not do its part; the similar innovations are shown regardless. */
export const AI_STATUS_NOTICES: Record<string, string> = {
  UNAVAILABLE:
    'Asystent AI jest chwilowo niedostępny albo zajęty innymi pytaniami. Poniżej widzisz tylko podobne innowacje z bazy. Spróbuj ponownie za chwilę.',
  INVALID_OUTPUT:
    'Asystent AI nie przygotował czytelnej odpowiedzi. Spróbuj ponownie, a jeśli się powtórzy, opisz pomysł nieco inaczej.',
}

export function closenessLabel(score: number): string {
  if (score >= 0.65) return 'Bardzo podobne'
  if (score >= 0.4) return 'Podobne'
  return 'Powiązane'
}
