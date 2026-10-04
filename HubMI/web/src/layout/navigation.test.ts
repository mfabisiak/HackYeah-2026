import { describe, expect, it } from 'vitest'
import { buildNavigation, initials, roleLabel, type NavEntry } from './navigation'

const labels = (entries: NavEntry[]) => entries.map((entry) => (entry.kind === 'link' ? entry.item.label : entry.label))
const groupItems = (entries: NavEntry[], label: string) =>
  entries.flatMap((entry) => (entry.kind === 'group' && entry.label === label ? entry.items.map((i) => i.label) : []))

describe('buildNavigation', () => {
  it('offers a visitor the public places only', () => {
    const entries = buildNavigation({ authenticated: false, isAdmin: false, isExpert: false })

    expect(labels(entries)).toEqual(['Opisz problem', 'Baza innowacji', 'Wiedza', 'Pomysły i granty'])
    expect(groupItems(entries, 'Pomysły i granty')).toEqual(['Asystent pomysłów', 'Nabory'])
  })

  it('adds what needs a login once the visitor signs in', () => {
    const entries = buildNavigation({ authenticated: true, isAdmin: false, isExpert: false })

    expect(labels(entries)).toContain('Wiadomości')
    expect(groupItems(entries, 'Pomysły i granty')).toEqual([
      'Asystent pomysłów',
      'Nabory',
      'Moje pomysły',
      'Moje wnioski',
      'Moje plany',
    ])
  })

  it('gives each official the own panel, and the admin only the admin one', () => {
    expect(labels(buildNavigation({ authenticated: true, isAdmin: true, isExpert: false }))).toContain('Panel admina')
    expect(labels(buildNavigation({ authenticated: true, isAdmin: false, isExpert: true }))).toContain('Panel eksperta')

    const both = labels(buildNavigation({ authenticated: true, isAdmin: true, isExpert: true }))
    expect(both).toContain('Panel admina')
    expect(both).not.toContain('Panel eksperta')
  })

  it('shows no panel to somebody who is not signed in', () => {
    const entries = labels(buildNavigation({ authenticated: false, isAdmin: true, isExpert: true }))

    expect(entries).not.toContain('Panel admina')
    expect(entries).not.toContain('Panel eksperta')
  })
})

describe('roleLabel and initials', () => {
  it('names the highest role', () => {
    expect(roleLabel(['user', 'expert', 'admin'])).toBe('Administrator ROPS')
    expect(roleLabel(['user', 'expert'])).toBe('Ekspert ROPS')
    expect(roleLabel(['user'])).toBe('Użytkownik')
  })

  it('takes up to two initials of a username', () => {
    expect(initials('jan.kowalski')).toBe('JK')
    expect(initials('anna')).toBe('A')
    expect(initials('a.b.c')).toBe('AB')
    expect(initials(undefined)).toBe('')
  })
})
