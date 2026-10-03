import { Alert, Badge, Button, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../api/hubApi'
import { useAuth } from '../auth/AuthContext'

interface Me {
  username: string | undefined
  email: string | undefined
  roles: string[]
}

type State = { kind: 'loading' } | { kind: 'ok'; me: Me } | { kind: 'error'; message: string }

export function AccountPage() {
  const { ready, authenticated, login } = useAuth()
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    if (!authenticated) return
    let cancelled = false
    void hubApi.me().then((result) => {
      if (cancelled) return
      const me = result.value
      if (result.error || !me) {
        setState({ kind: 'error', message: result.error?.message ?? 'Nie udało się pobrać danych konta.' })
      } else {
        setState({
          kind: 'ok',
          me: { username: me.username ?? undefined, email: me.email ?? undefined, roles: Array.from(me.roles) },
        })
      }
    })
    return () => {
      cancelled = true
    }
  }, [authenticated])

  if (!ready) return <Loader aria-label="Ładowanie" />

  if (!authenticated) {
    return (
      <Stack gap="md" maw={640}>
        <Title order={2}>Moje konto</Title>
        <Text>Zaloguj się, aby zobaczyć dane konta.</Text>
        <div>
          <Button onClick={login}>Zaloguj się</Button>
        </div>
      </Stack>
    )
  }

  return (
    <Stack gap="md" maw={640}>
      <Title order={2}>Moje konto</Title>
      <div role="status" aria-live="polite">
        {state.kind === 'loading' && <Loader aria-label="Ładowanie danych konta" />}
        {state.kind === 'error' && (
          <Alert color="red" icon={<IconAlertTriangle aria-hidden />} title="Błąd">
            {state.message}
          </Alert>
        )}
      </div>
      {state.kind === 'ok' && (
        <Stack gap="xs" component="dl" m={0}>
          <Text component="dt" fw={600}>
            Nazwa użytkownika
          </Text>
          <Text component="dd" m={0}>
            {state.me.username}
          </Text>
          <Text component="dt" fw={600}>
            Adres e-mail
          </Text>
          <Text component="dd" m={0}>
            {state.me.email}
          </Text>
          <Text component="dt" fw={600}>
            Role
          </Text>
          <Group component="dd" m={0} gap="xs">
            {state.me.roles.map((role) => (
              <Badge key={role} variant="light">
                {role}
              </Badge>
            ))}
          </Group>
        </Stack>
      )}
    </Stack>
  )
}
