import { Anchor, Button, Container, Group, Text, Title, useComputedColorScheme, useMantineColorScheme } from '@mantine/core'
import { IconMoon, IconSun } from '@tabler/icons-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useRef } from 'react'
import { useAuth } from '../auth/AuthContext'

const links = [
  { to: '/', label: 'Start' },
  { to: '/status', label: 'Status systemu' },
  { to: '/konto', label: 'Moje konto' },
]

export function Shell() {
  const { ready, authenticated, username, login, logout } = useAuth()
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
                {links.map((link) => (
                  <li key={link.to}>
                    <Anchor component={NavLink} to={link.to} end underline="hover" fw={500}>
                      {link.label}
                    </Anchor>
                  </li>
                ))}
              </Group>
            </nav>
            <Group gap="sm">
              {ready && authenticated ? (
                <>
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
