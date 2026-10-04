import { fireEvent, render, screen, within } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SiteHeader } from './SiteHeader'

interface MockAuth {
  ready: boolean
  authenticated: boolean
  username: string | undefined
  roles: string[]
  hasRole: (role: string) => boolean
  login: () => void
  logout: () => void
}

let mockAuth = signedOut()

function signedOut(): MockAuth {
  return {
    ready: true,
    authenticated: false,
    username: undefined,
    roles: [],
    hasRole: () => false,
    login: vi.fn(),
    logout: vi.fn(),
  }
}

function signedInAs(roles: string[]): MockAuth {
  return {
    ...signedOut(),
    authenticated: true,
    username: 'anna.nowak',
    roles,
    hasRole: (role: string) => roles.includes(role),
  }
}

vi.mock('../auth/AuthContext', () => ({ useAuth: () => mockAuth }))
vi.mock('../features/messaging/NotificationBell', () => ({ NotificationBell: () => <button>Powiadomienia</button> }))

function renderHeader(path = '/') {
  return render(
    <MantineProvider>
      <MemoryRouter initialEntries={[path]}>
        <SiteHeader />
      </MemoryRouter>
    </MantineProvider>,
  )
}

describe('SiteHeader', () => {
  beforeEach(() => {
    mockAuth = signedOut()
  })

  it('has the brand as a link home and no heading of its own', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'HubMI, strona główna' })).toHaveAttribute('href', '/')
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
  })

  it('lists the public places for a visitor and offers to sign in', () => {
    renderHeader()

    const nav = screen.getByRole('navigation', { name: 'Główna nawigacja' })
    expect(within(nav).getByRole('link', { name: 'Opisz problem' })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: 'Baza innowacji' })).toBeInTheDocument()
    expect(within(nav).getByRole('button', { name: 'Wiedza' })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: 'Wiadomości' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Zaloguj się/ })).toBeInTheDocument()
  })

  it('marks the current page for assistive technology', () => {
    renderHeader('/innowacje')

    const nav = screen.getByRole('navigation', { name: 'Główna nawigacja' })
    expect(within(nav).getByRole('link', { name: 'Baza innowacji' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'Opisz problem' })).not.toHaveAttribute('aria-current')
  })

  it('opens a group as a menu of links', async () => {
    mockAuth = signedInAs(['user'])
    renderHeader()

    const trigger = screen.getByRole('button', { name: 'Pomysły i granty' })
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(trigger)

    expect(trigger).toHaveAttribute('aria-expanded', 'true')
    const items = (await screen.findAllByRole('menuitem', { hidden: true })).map((item) => item.textContent)
    expect(items).toEqual(['Asystent pomysłów', 'Nabory', 'Moje pomysły', 'Moje wnioski', 'Moje plany'])
  })

  it('shows an official the panel of the role, and the admin the admin panel', () => {
    mockAuth = signedInAs(['user', 'expert'])
    const expert = renderHeader()
    expect(screen.getByRole('link', { name: 'Panel eksperta' })).toHaveAttribute('href', '/ekspert')
    expect(screen.queryByRole('link', { name: 'Panel admina' })).not.toBeInTheDocument()
    expert.unmount()

    mockAuth = signedInAs(['user', 'admin'])
    renderHeader()
    expect(screen.getByRole('link', { name: 'Panel admina' })).toHaveAttribute('href', '/admin')
  })

  it('tells who is signed in and signs out from the account menu', async () => {
    mockAuth = signedInAs(['user', 'expert'])
    renderHeader()

    fireEvent.click(screen.getByRole('button', { name: 'Konto: anna.nowak, Ekspert ROPS' }))
    expect(await screen.findByRole('menuitem', { name: 'Moje konto', hidden: true })).toHaveAttribute('href', '/konto')

    fireEvent.click(screen.getByRole('menuitem', { name: 'Wyloguj', hidden: true }))
    expect(mockAuth.logout).toHaveBeenCalled()
  })

  it('opens the whole navigation in a drawer on a phone', async () => {
    mockAuth = signedInAs(['user'])
    renderHeader()

    const burger = screen.getByRole('button', { name: 'Otwórz menu' })
    expect(burger).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(burger)

    const drawer = await screen.findByRole('dialog', { hidden: true })
    expect(within(drawer).getByRole('link', { name: 'Moje wnioski' })).toBeInTheDocument()
    expect(within(drawer).getByRole('link', { name: 'Status systemu' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Zamknij menu' })).toHaveAttribute('aria-expanded', 'true')
  })
})
