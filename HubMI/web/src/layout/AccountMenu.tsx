import { Avatar, Button, Menu, Text } from '@mantine/core'
import { IconCheck, IconChevronDown, IconLogout, IconUser } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ACCOUNT_LINKS, initials, roleLabel } from './navigation'

/** Who is signed in, the account links and the way out; in the demo also the switch between the demo accounts. */
export function AccountMenu() {
  const { ready, authenticated, username, roles, login, logout, demo } = useAuth()

  if (!authenticated) {
    if (!demo) {
      return (
        <Button onClick={login} disabled={!ready} leftSection={<IconUser aria-hidden size={18} />}>
          Zaloguj się
        </Button>
      )
    }
    return (
      <Menu position="bottom-end" withinPortal width={260}>
        <Menu.Target>
          <Button rightSection={<IconChevronDown aria-hidden size={16} />}>Zaloguj się jako…</Button>
        </Menu.Target>
        <Menu.Dropdown>
          <DemoAccounts />
        </Menu.Dropdown>
      </Menu>
    )
  }

  const role = roleLabel(roles)

  return (
    <Menu position="bottom-end" withinPortal width={260}>
      <Menu.Target>
        <Button
          variant="default"
          px="xs"
          aria-label={`Konto: ${username ?? 'użytkownik'}, ${role}`}
          leftSection={
            <Avatar size={28} radius="xl" color="blue" variant="filled" aria-hidden>
              {initials(username)}
            </Avatar>
          }
          rightSection={<IconChevronDown aria-hidden size={16} />}
          styles={{ section: { marginInline: 6 } }}
        />
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>
          Zalogowano jako
          <Text fw={700} c="var(--mantine-color-text)" size="sm" lh={1.3}>
            {username}
          </Text>
          <Text size="xs" c="dimmed">
            {role}
          </Text>
        </Menu.Label>
        {ACCOUNT_LINKS.map((link) => (
          <Menu.Item key={link.to} component={Link} to={link.to}>
            {link.label}
          </Menu.Item>
        ))}
        {demo && (
          <>
            <Menu.Divider />
            <DemoAccounts />
          </>
        )}
        <Menu.Divider />
        <Menu.Item onClick={logout} leftSection={<IconLogout aria-hidden size={16} />}>
          Wyloguj
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  )
}

/** In the demo there is no Keycloak: one picks the role to sign in as, and can switch to another any time. */
function DemoAccounts() {
  const { authenticated, demo } = useAuth()
  if (!demo) return null

  return (
    <>
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
    </>
  )
}
