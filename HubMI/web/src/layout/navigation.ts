export interface NavItem {
  to: string
  label: string
}

export type NavEntry = { kind: 'link'; item: NavItem } | { kind: 'group'; label: string; items: NavItem[] }

export interface NavAudience {
  authenticated: boolean
  isAdmin: boolean
  isExpert: boolean
}

/** Links of the account menu; they are not part of the main navigation. */
export const ACCOUNT_LINKS: NavItem[] = [
  { to: '/konto', label: 'Moje konto' },
  { to: '/status', label: 'Status systemu' },
]

/** What the main navigation offers to the given visitor; the entries without a login or a role are left out. */
export function buildNavigation({ authenticated, isAdmin, isExpert }: NavAudience): NavEntry[] {
  const link = (to: string, label: string): NavEntry => ({ kind: 'link', item: { to, label } })
  const ideasAndGrants: NavItem[] = [
    { to: '/asystent', label: 'Asystent pomysłów' },
    { to: '/nabory', label: 'Nabory' },
    ...(authenticated
      ? [
          { to: '/pomysly', label: 'Moje pomysły' },
          { to: '/wnioski', label: 'Moje wnioski' },
          { to: '/moje-plany', label: 'Moje plany' },
        ]
      : []),
  ]

  return [
    link('/dopasuj', 'Opisz problem'),
    link('/innowacje', 'Baza innowacji'),
    {
      kind: 'group',
      label: 'Wiedza',
      items: [
        { to: '/wyzwania', label: 'Wyzwania Małopolski' },
        { to: '/materialy', label: 'Materiały edukacyjne' },
      ],
    },
    { kind: 'group', label: 'Pomysły i granty', items: ideasAndGrants },
    ...(authenticated ? [link('/wiadomosci', 'Wiadomości')] : []),
    ...(authenticated && isAdmin ? [link('/admin', 'Panel admina')] : []),
    ...(authenticated && !isAdmin && isExpert ? [link('/ekspert', 'Panel eksperta')] : []),
  ]
}

/** How the roles of the signed-in user are called in the interface. */
export function roleLabel(roles: readonly string[]): string {
  if (roles.includes('admin')) return 'Administrator ROPS'
  if (roles.includes('expert')) return 'Ekspert ROPS'
  return 'Użytkownik'
}

/** Up to two initials of a username such as `jan.kowalski`, for the avatar. */
export function initials(username: string | undefined): string {
  const parts = (username ?? '').split(/[.\s_-]+/).filter(Boolean)
  return parts
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
}
