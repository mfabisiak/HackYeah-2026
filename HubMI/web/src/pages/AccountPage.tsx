import { Alert, Badge, Button, Group, Paper, Stack, Text } from '@mantine/core'
import { IconAlertCircle } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { toFriendlyErrorMessage } from '../api/errors'
import { hubApi } from '../api/hubApi'
import { useAuth } from '../auth/AuthContext'
import { keycloak } from '../auth/keycloak'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorAlert } from '../components/ErrorAlert'

interface Me {
  username: string | undefined
  email: string | undefined
  roles: string[]
}

type State =
  | { kind: 'loading' }
  | { kind: 'ok'; me: Me }
  | { kind: 'session_expired' }
  | { kind: 'error'; message: string }

export function AccountPage() {
  const { ready, authenticated, login } = useAuth()
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    if (!authenticated) return
    let cancelled = false

    const loadMe = async () => {
      // If token is expired or about to expire in the next 10s, attempt refresh
      if (typeof keycloak.isTokenExpired === 'function' && keycloak.isTokenExpired(10)) {
        try {
          await keycloak.updateToken(30)
        } catch {
          if (!cancelled) {
            setState({ kind: 'session_expired' })
          }
          return
        }
      }

      const result = await hubApi.me()
      if (cancelled) return

      if (result.error) {
        const isUnauthorized =
          result.error.status === 401 ||
          result.error.code === 'UNAUTHORIZED' ||
          (typeof result.error.message === 'string' &&
            result.error.message.toLowerCase().includes('unauthorized'))

        if (isUnauthorized) {
          // Attempt token update once more in case clock skew caused 401
          try {
            await keycloak.updateToken(-1)
            const retryResult = await hubApi.me()
            if (cancelled) return
            if (!retryResult.error && retryResult.value) {
              const me = retryResult.value
              setState({
                kind: 'ok',
                me: {
                  username: me.username ?? undefined,
                  email: me.email ?? undefined,
                  roles: Array.from(me.roles),
                },
              })
              return
            }
          } catch {
            // refresh token expired as well
          }

          setState({ kind: 'session_expired' })
          return
        }

        setState({
          kind: 'error',
          message: toFriendlyErrorMessage(result.error, 'Nie udało się pobrać danych konta.'),
        })
        return
      }

      const me = result.value
      if (!me) {
        setState({ kind: 'error', message: 'Nie udało się pobrać danych konta.' })
        return
      }

      setState({
        kind: 'ok',
        me: {
          username: me.username ?? undefined,
          email: me.email ?? undefined,
          roles: Array.from(me.roles),
        },
      })
    }

    void loadMe()

    return () => {
      cancelled = true
    }
  }, [authenticated])

  if (!ready) return <LoadingState message="Ładowanie informacji o profilu..." />

  if (!authenticated) {
    return (
      <Stack gap="md" maw={640}>
        <PageHeader
          title="Moje konto"
          subtitle="Zaloguj się, aby zarządzać swoim profilem i uprawnieniami."
          breadcrumbs={[
            { title: 'Strona główna', href: '/' },
            { title: 'Moje konto' },
          ]}
        />
        <Paper withBorder p="lg" radius="md">
          <Stack gap="md">
            <Text size="md">
              Nie jesteś obecnie zalogowany. Aby zobaczyć dane swojego profilu i uprawnienia, zaloguj się do systemu.
            </Text>
            <div>
              <Button size="md" onClick={login}>
                Zaloguj się
              </Button>
            </div>
          </Stack>
        </Paper>
      </Stack>
    )
  }

  return (
    <Stack gap="md" maw={640}>
      <PageHeader
        title="Moje konto"
        subtitle="Szczegóły profilu użytkownika i przypisane role w systemie."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Moje konto' },
        ]}
      />
      {state.kind === 'loading' && <LoadingState message="Pobieranie danych konta..." />}
      {state.kind === 'session_expired' && (
        <Alert
          color="yellow"
          title="Twoja sesja wygasła"
          icon={<IconAlertCircle size={22} aria-hidden="true" />}
          radius="md"
          styles={{
            title: { fontSize: '1.1rem', fontWeight: 700 },
          }}
        >
          <Stack gap="md" mt="xs">
            <Text size="md" style={{ lineHeight: 1.5 }}>
              Twoja sesja wygasła z powodu dłuższego braku aktywności. Aby wyświetlić dane swojego konta i kontynuować pracę, zaloguj się ponownie.
            </Text>
            <div>
              <Button size="md" onClick={login}>
                Zaloguj się ponownie
              </Button>
            </div>
          </Stack>
        </Alert>
      )}
      {state.kind === 'error' && (
        <ErrorAlert message={state.message} />
      )}
      {state.kind === 'ok' && (
        <Paper withBorder p="lg" radius="md">
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
        </Paper>
      )}
    </Stack>
  )
}
