import {
  ActionIcon,
  Box,
  Burger,
  Container,
  Drawer,
  Menu,
  Stack,
  useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core'
import { IconBulb, IconChevronDown, IconMoon, IconSun } from '@tabler/icons-react'
import { useId, useState } from 'react'
import { Link, NavLink, matchPath, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { NotificationBell } from '../features/messaging/NotificationBell'
import { AccountMenu } from './AccountMenu'
import { ACCOUNT_LINKS, buildNavigation, type NavEntry, type NavItem } from './navigation'

// Below 76em (1216px at the default font size) the navigation moves to a drawer: the full list, with the panel link
// of an official, needs about 1000px next to the brand and the account area. The limit is in em so that visitors
// who enlarge the text get the drawer sooner, instead of a navigation broken into two rows (see layout.css).

const isActive = (item: NavItem, pathname: string) => matchPath({ path: item.to, end: item.to === '/' }, pathname) !== null

export function SiteHeader() {
  const { authenticated, hasRole } = useAuth()
  const entries = buildNavigation({ authenticated, isAdmin: hasRole('admin'), isExpert: hasRole('expert') })

  return (
    <header className="site-header">
      <Container size="xl" className="site-header__bar">
        <Link to="/" className="brand" aria-label="HubMI, strona główna">
          <span className="brand__mark" aria-hidden="true">
            <IconBulb size={26} stroke={1.8} />
          </span>
          <span className="brand__name">HubMI</span>
        </Link>

        <Box
          component="nav"
          aria-label="Główna nawigacja"
          className="site-header__nav show-desktop"
          data-testid="desktop-nav"
        >
          <ul className="nav-list">
            {entries.map((entry) => (
              <li key={entry.kind === 'link' ? entry.item.to : entry.label}>
                <DesktopEntry entry={entry} />
              </li>
            ))}
          </ul>
        </Box>

        <div className="site-header__actions">
          {authenticated && <NotificationBell />}
          <ThemeToggle className="show-desktop" />
          <AccountMenu />
          <MobileMenu entries={entries} />
        </div>
      </Container>
    </header>
  )
}

function DesktopEntry({ entry }: { entry: NavEntry }) {
  const { pathname } = useLocation()

  if (entry.kind === 'link') {
    const staff = entry.item.to === '/admin' || entry.item.to === '/ekspert'
    return (
      <NavLink
        to={entry.item.to}
        end={entry.item.to === '/'}
        className={staff ? 'nav-link nav-link--staff' : 'nav-link'}
      >
        {entry.item.label}
      </NavLink>
    )
  }

  const active = entry.items.some((item) => isActive(item, pathname))
  return (
    <Menu position="bottom-start" withinPortal width={250}>
      <Menu.Target>
        <button type="button" className="nav-link" data-active={active || undefined}>
          {entry.label}
          <IconChevronDown className="nav-link__chevron" aria-hidden size={16} />
        </button>
      </Menu.Target>
      <Menu.Dropdown>
        {entry.items.map((item) => (
          <Menu.Item key={item.to} component={NavLink} to={item.to}>
            {item.label}
          </Menu.Item>
        ))}
      </Menu.Dropdown>
    </Menu>
  )
}

function ThemeToggle({ className }: { className?: string }) {
  const { setColorScheme } = useMantineColorScheme()
  const scheme = useComputedColorScheme('light')
  const dark = scheme === 'dark'
  const label = dark ? 'Włącz jasny motyw' : 'Włącz ciemny motyw'

  return (
    <ActionIcon
      variant="default"
      size={44}
      onClick={() => setColorScheme(dark ? 'light' : 'dark')}
      aria-label={label}
      title={label}
      className={className}
    >
      {dark ? <IconSun aria-hidden size={20} /> : <IconMoon aria-hidden size={20} />}
    </ActionIcon>
  )
}

function MobileMenu({ entries }: { entries: NavEntry[] }) {
  const { pathname } = useLocation()
  // The drawer is open for the page it was opened on, so going to another page closes it.
  const [openedOn, setOpenedOn] = useState<string | null>(null)
  const opened = openedOn === pathname
  const setOpened = (open: boolean) => setOpenedOn(open ? pathname : null)
  const drawerId = useId()

  const sections = entries.reduce<{ title?: string; items: NavItem[] }[]>((acc, entry) => {
    if (entry.kind === 'group') return [...acc, { title: entry.label, items: entry.items }]
    const last = acc[acc.length - 1]
    return last && last.title === undefined
      ? [...acc.slice(0, -1), { items: [...last.items, entry.item] }]
      : [...acc, { items: [entry.item] }]
  }, [])

  return (
    <>
      <Burger
        opened={opened}
        onClick={() => setOpened(!opened)}
        className="hide-desktop"
        size="md"
        aria-label={opened ? 'Zamknij menu' : 'Otwórz menu'}
        aria-expanded={opened}
        aria-controls={drawerId}
        style={{ minWidth: 44, minHeight: 44 }}
      />
      <Drawer
        id={drawerId}
        opened={opened}
        onClose={() => setOpened(false)}
        position="right"
        size="min(340px, 100%)"
        title="Menu"
      >
        <nav aria-label="Główna nawigacja (menu)">
          <Stack gap="lg">
            {sections.map((section, index) => (
              <div key={section.title ?? index}>
                {section.title && <p className="mobile-nav__group-title">{section.title}</p>}
                <ul className="mobile-nav__list">
                  {section.items.map((item) => (
                    <li key={item.to}>
                      <NavLink to={item.to} end={item.to === '/'} className="nav-link">
                        {item.label}
                      </NavLink>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
            <div>
              <p className="mobile-nav__group-title">Konto i ustawienia</p>
              <ul className="mobile-nav__list">
                {ACCOUNT_LINKS.map((item) => (
                  <li key={item.to}>
                    <NavLink to={item.to} className="nav-link">
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
              <Stack mt="md" px="xs">
                <ThemeToggleButton />
              </Stack>
            </div>
          </Stack>
        </nav>
      </Drawer>
    </>
  )
}

function ThemeToggleButton() {
  const { setColorScheme } = useMantineColorScheme()
  const scheme = useComputedColorScheme('light')
  const dark = scheme === 'dark'

  return (
    <button type="button" className="nav-link" onClick={() => setColorScheme(dark ? 'light' : 'dark')}>
      {dark ? <IconSun aria-hidden size={18} /> : <IconMoon aria-hidden size={18} />}
      {dark ? 'Włącz jasny motyw' : 'Włącz ciemny motyw'}
    </button>
  )
}
