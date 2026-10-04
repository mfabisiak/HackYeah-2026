export const NOTIFICATION_TYPE_CONFIG: Record<
  string,
  { label: string; color: string }
> = {
  IDEA_SUBMITTED: { label: 'Zgłoszenie pomysłu', color: 'blue' },
  IDEA_STATUS_CHANGED: { label: 'Status pomysłu', color: 'teal' },
  CALL_PUBLISHED: { label: 'Nowy nabór wniosków', color: 'indigo' },
  CALL_CHANGED: { label: 'Aktualizacja naboru', color: 'cyan' },
  MESSAGE_RECEIVED: { label: 'Wiadomość z ROPS', color: 'violet' },
}

export const PARTICIPANT_ROLE_CONFIG: Record<
  string,
  { label: string; badgeColor: string; description: string }
> = {
  ADMIN: {
    label: 'Pracownik ROPS',
    badgeColor: 'blue',
    description: 'Regionalny Ośrodek Polityki Społecznej w Krakowie',
  },
  AUTHOR: {
    label: 'Ty (Autor)',
    badgeColor: 'gray',
    description: 'Nadawca wiadomości',
  },
  EXPERT: {
    label: 'Ekspert merytoryczny',
    badgeColor: 'teal',
    description: 'Konsultant regionalny',
  },
}

export function formatPolishDateTime(isoDateString?: string | null): string {
  if (!isoDateString) return 'Brak daty'
  try {
    const d = new Date(isoDateString)
    if (isNaN(d.getTime())) return isoDateString
    return new Intl.DateTimeFormat('pl-PL', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(d)
  } catch {
    return isoDateString
  }
}

export function formatRelativePolishTime(isoDateString?: string | null): string {
  if (!isoDateString) return ''
  try {
    const d = new Date(isoDateString)
    if (isNaN(d.getTime())) return isoDateString
    const now = Date.now()
    const diffMs = now - d.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return 'przed chwilą'
    if (diffMins === 1) return '1 minutę temu'
    if (diffMins < 60) return `${diffMins} min temu`
    if (diffHours === 1) return '1 godzinę temu'
    if (diffHours < 24) return `${diffHours} godz. temu`
    if (diffDays === 1) return 'wczoraj'
    if (diffDays < 7) return `${diffDays} dni temu`
    return formatPolishDateTime(isoDateString)
  } catch {
    return isoDateString
  }
}
