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

/**
 * Declension helper for Polish "sprawa oczekująca"
 * 1 sprawę oczekującą
 * 2, 3, 4 sprawy oczekujące
 * 5 spraw oczekujących
 * 22 sprawy oczekujące, 25 spraw oczekujących, 112 spraw oczekujących
 */
export function pluralizeSprawy(count: number): {
  count: number
  noun: string
  adjective: string
  fullPhrase: string
} {
  const abs = Math.abs(count)
  if (abs === 1) {
    return {
      count,
      noun: 'sprawę',
      adjective: 'oczekującą',
      fullPhrase: '1 sprawę oczekującą',
    }
  }

  const lastDigit = abs % 10
  const lastTwoDigits = abs % 100

  if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) {
    return {
      count,
      noun: 'sprawy',
      adjective: 'oczekujące',
      fullPhrase: `${count} sprawy oczekujące`,
    }
  }

  return {
    count,
    noun: 'spraw',
    adjective: 'oczekujących',
    fullPhrase: `${count} spraw oczekujących`,
  }
}

/**
 * Senior-friendly badge styling that prevents Mantine ellipsis truncation (...)
 * and allows full Polish text wrapping with accessible font size and height.
 */
export const accessibleBadgeStyles = {
  root: {
    height: 'auto',
    minHeight: 28,
    paddingTop: 4,
    paddingBottom: 4,
    paddingInline: 10,
    whiteSpace: 'normal' as const,
  },
  label: {
    whiteSpace: 'normal' as const,
    overflow: 'visible' as const,
    textOverflow: 'clip' as const,
    fontSize: '0.88rem',
    fontWeight: 600,
    lineHeight: 1.3,
    textAlign: 'center' as const,
  },
}

