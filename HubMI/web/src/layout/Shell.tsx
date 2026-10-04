import {
  Alert,
  Anchor,
  Button,
  Container,
  Group,
  Menu,
  Text,
  Title,
  useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core'
import { IconCheck, IconChevronDown, IconMoon, IconSun } from '@tabler/icons-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useRef } from 'react'
import { resetMockHubData } from 'hubmi-client'
import { useAuth } from '../auth/AuthContext'
import { MOCK_MODE } from '../auth/keycloak'
import { NotificationBell } from '../features/messaging/NotificationBell'

const contentLinks = [
  { to: '/', label: 'Start' },
  { to: '/dopasuj', label: 'Opisz problem' },
  { to: '/asystent', label: 'Asystent pomysłów' },
  { to: '/innowacje', label: 'Baza innowacji' },
  { to: '/wyzwania', label: 'Wyzwania Małopolski' },
  { to: '/materialy', label: 'Materiały edukacyjne' },
  { to: '/pomysly', label: 'Moje pomysły' },
  { to: '/nabory', label: 'Nabory' },
  { to: '/wnioski', label: 'Moje wnioski' },
]

const accountLinks = [
  { to: '/status', label: 'Status' },
  { to: '/konto', label: 'Moje konto' },
]

export function Shell() {
  const { ready, authenticated, username, login, logout, hasRole, demo } = useAuth()
  const navLinks = [
    ...contentLinks,
    ...(authenticated
      ? [
          { to: '/wiadomosci', label: 'Wiadomości z ROPS' },
          { to: '/moje-plany', label: 'Moje plany' },
        ]
      : []),
    ...accountLinks,
    ...(authenticated && hasRole('admin') ? [{ to: '/admin', label: 'Panel admina' }] : []),
    ...(authenticated && !hasRole('admin') && hasRole('expert') ? [{ to: '/ekspert', label: 'Panel eksperta' }] : []),
  ]
  const { setColorScheme } = useMantineColorScheme()
  const scheme = useComputedColorScheme('light')
  const mainRef = useRef<HTMLElement>(null)
  const { pathname } = useLocation()

  // Move focus to the page content on navigation so screen-reader and keyboard users land on the new view.
  useEffect(() => {
    mainRef.current?.focus()
  }, [pathname])

  return (
    <>
      <a className="skip-link" href="#main">
        Przejdź do treści
      </a>
      <header>
        <Container size="lg" py="md">
          <Group justify="space-between" wrap="wrap" gap="md">
            <Title order={1} size="h3">
              HubMI
            </Title>
            <nav aria-label="Główna nawigacja">
              <Group component="ul" gap="md" p={0} m={0} style={{ listStyle: 'none' }}>
                {navLinks.map((link) => (
                  <li key={link.to}>
                    <Anchor component={NavLink} to={link.to} end={link.to === '/'} underline="hover" fw={500}>
                      {link.label}
                    </Anchor>
                  </li>
                ))}
              </Group>
            </nav>
            <Group gap="sm">
              {demo ? (
                <>
                  {authenticated && <NotificationBell />}
                  <DemoAccountMenu />
                </>
              ) : ready && authenticated ? (
                <>
                  <NotificationBell />
                  <Text span>Zalogowano: {username}</Text>
                  <Button variant="default" onClick={logout}>
                    Wyloguj
                  </Button>
                </>
              ) : (
                <Button onClick={login} disabled={!ready}>
                  Zaloguj się
                </Button>
              )}
              <Button
                variant="default"
                onClick={() => setColorScheme(scheme === 'dark' ? 'light' : 'dark')}
                aria-label={scheme === 'dark' ? 'Włącz jasny motyw' : 'Włącz ciemny motyw'}
                leftSection={scheme === 'dark' ? <IconSun aria-hidden size={18} /> : <IconMoon aria-hidden size={18} />}
              >
                {scheme === 'dark' ? 'Jasny' : 'Ciemny'}
              </Button>
            </Group>
          </Group>
        </Container>
      </header>
      {MOCK_MODE && <DemoNotice />}
      <main id="main" ref={mainRef} tabIndex={-1} style={{ outline: 'none' }}>
        <Container size="lg" py="xl">
          <Outlet />
        </Container>
      </main>
      <footer>
        <Container size="lg" py="md">
          <Text size="sm" c="dimmed">
            Prototyp na potrzeby HackYeah 2026 · dane przykładowe
          </Text>
        </Container>
      </footer>
    </>
  )
}

/** The demo has no server: tells what it runs on, and lets the presenter start over. */
function DemoNotice() {
  const startOver = () => {
    resetMockHubData()
    window.location.reload()
  }

  return (
    <Container size="lg" pt="md">
      <Alert color="blue" variant="light" title="Wersja demonstracyjna" role="note">
        <Group justify="space-between" gap="md">
          <Text size="sm" maw={720}>
            Aplikacja działa bez serwera, na danych przykładowych. To, co dodasz (pomysły, oceny, wnioski), zapisuje się
            tylko w tej przeglądarce.
          </Text>
          <Button variant="default" size="xs" onClick={startOver}>
            Przywróć dane początkowe
          </Button>
        </Group>
      </Alert>
    </Container>
  )
}

/** In the demo there is no Keycloak: one picks the role to sign in as, and can switch to another any time. */
function DemoAccountMenu() {
  const { authenticated, logout, demo } = useAuth()
  if (!demo) return null

  return (
    <Menu position="bottom-end" withinPortal>
      <Menu.Target>
        <Button variant={authenticated ? 'default' : 'filled'} rightSection={<IconChevronDown aria-hidden size={16} />}>
          {authenticated ? `Zalogowano: ${demo.current.username} (${demo.current.label})` : 'Zaloguj się jako…'}
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{authenticated ? 'Zmień konto demo' : 'Wybierz konto demo'}</Menu.Label>
        {demo.accounts.map((account) => {
          const isCurrent = authenticated && account.role === demo.current.role
          return (
            <Menu.Item
              key={account.role}
              onClick={() => demo.signInAs(account.role)}
              rightSection={isCurrent ? <IconCheck aria-hidden size={16} /> : undefined}
              aria-current={isCurrent ? 'true' : undefined}
            >
              {account.label}
              <Text size="xs" c="dimmed">
                {account.username}
              </Text>
            </Menu.Item>
          )
        })}
        {authenticated && (
          <>
            <Menu.Divider />
            <Menu.Item onClick={logout}>Wyloguj</Menu.Item>
          </>
        )}
      </Menu.Dropdown>
    </Menu>
  )
}
