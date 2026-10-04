import { Anchor, Button, Container, Group, Text, Title, useComputedColorScheme, useMantineColorScheme } from '@mantine/core'
import { IconMoon, IconSun } from '@tabler/icons-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useRef } from 'react'
import { useAuth } from '../auth/AuthContext'
import { NotificationBell } from '../features/messaging/NotificationBell'

const contentLinks = [
  { to: '/', label: 'Start' },
  { to: '/dopasuj', label: 'Opisz problem' },
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
  const { ready, authenticated, username, login, logout, hasRole } = useAuth()
  const navLinks = [
    ...contentLinks,
    ...(authenticated ? [{ to: '/wiadomosci', label: 'Wiadomości z ROPS' }] : []),
    ...accountLinks,
    ...(authenticated && (hasRole('expert') || hasRole('admin')) ? [{ to: '/ekspert', label: 'Strefa eksperta' }] : []),
    ...(authenticated && hasRole('admin') ? [{ to: '/admin', label: 'Panel admina' }] : []),
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
              {ready && authenticated ? (
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
