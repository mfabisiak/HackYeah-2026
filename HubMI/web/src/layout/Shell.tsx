import { Alert, Anchor, Button, Container, Group, Text } from '@mantine/core'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useLayoutEffect, useRef } from 'react'
import { resetMockHubData } from 'hubmi-client'
import { MOCK_MODE } from '../auth/keycloak'
import { SiteHeader } from './SiteHeader'
import './layout.css'

const DEFAULT_TITLE = 'HubMI – Małopolski Hub Innowacji Społecznych'

export function Shell() {
  const mainRef = useRef<HTMLElement>(null)
  const { pathname } = useLocation()
  const shownPath = useRef(pathname)

  // A page that sets no title of its own must not keep the one of the previous page (WCAG 2.4.2): the title is
  // reset before the new page runs its effects and, when it stays unset, taken from the heading of the page.
  useLayoutEffect(() => {
    document.title = DEFAULT_TITLE
  }, [pathname])

  // After a navigation screen-reader and keyboard users land on the new view, and everybody starts at its top.
  // Not on the first load: there the page simply opens at the top with the focus where the browser puts it.
  useEffect(() => {
    if (document.title === DEFAULT_TITLE) {
      const heading = mainRef.current?.querySelector('h1')?.textContent?.trim()
      if (heading && pathname !== '/') document.title = `${heading} | HubMI`
    }
    if (shownPath.current === pathname) return
    shownPath.current = pathname
    window.scrollTo(0, 0)
    mainRef.current?.focus({ preventScroll: true })
  }, [pathname])

  return (
    <>
      <a className="skip-link" href="#main">
        Przejdź do treści
      </a>
      <SiteHeader />
      {MOCK_MODE && <DemoNotice />}
      <main id="main" ref={mainRef} tabIndex={-1} style={{ outline: 'none' }}>
        <Container size="lg" py="xl">
          <Outlet />
        </Container>
      </main>
      <footer className="site-footer">
        <Container size="lg" py="lg">
          <Group justify="space-between" align="flex-start" gap="lg">
            <Text size="sm" c="dimmed" maw={420}>
              Prototyp na potrzeby HackYeah 2026 · dane przykładowe. Platforma Małopolskiego Hubu Innowacji Społecznych
              dla Regionalnego Ośrodka Polityki Społecznej w Krakowie.
            </Text>
            <nav aria-label="Informacje">
              <ul className="site-footer__links">
                <li>
                  <Anchor component={Link} to="/dostepnosc" size="sm">
                    Deklaracja dostępności
                  </Anchor>
                </li>
                <li>
                  <Anchor component={Link} to="/status" size="sm">
                    Status systemu
                  </Anchor>
                </li>
              </ul>
            </nav>
          </Group>
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
    <Container size="lg" pt="md" w="100%" component="aside" aria-label="Informacja o wersji demonstracyjnej">
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
