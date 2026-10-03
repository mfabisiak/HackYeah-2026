import { Alert, Button, Loader, Stack, Text, Title } from '@mantine/core'
import { IconAlertTriangle, IconCircleCheck } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { hubApi } from '../api/hubApi'

type State = { kind: 'loading' } | { kind: 'ok'; status: string } | { kind: 'error'; message: string }

export function StatusPage() {
  const [state, setState] = useState<State>({ kind: 'loading' })

  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    void hubApi.health().then((result) => {
      if (cancelled) return
      if (result.error) setState({ kind: 'error', message: result.error.message })
      else setState({ kind: 'ok', status: result.value?.status ?? 'UNKNOWN' })
    })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const recheck = () => {
    setState({ kind: 'loading' })
    setAttempt((n) => n + 1)
  }

  return (
    <Stack gap="md" maw={640}>
      <Title order={2}>Status systemu</Title>
      <Text>Ta strona pyta serwer o stan przez klienta wygenerowanego w Kotlinie.</Text>
      <div role="status" aria-live="polite">
        {state.kind === 'loading' && (
          <Alert icon={<Loader size={18} aria-hidden />} title="Sprawdzanie…">
            Trwa łączenie z serwerem.
          </Alert>
        )}
        {state.kind === 'ok' && (
          <Alert color="green" icon={<IconCircleCheck aria-hidden />} title="Serwer działa">
            Odpowiedź serwera: {state.status}
          </Alert>
        )}
        {state.kind === 'error' && (
          <Alert color="red" icon={<IconAlertTriangle aria-hidden />} title="Brak połączenia z serwerem">
            {state.message}
          </Alert>
        )}
      </div>
      <div>
        <Button variant="default" onClick={recheck} disabled={state.kind === 'loading'}>
          Sprawdź ponownie
        </Button>
      </div>
    </Stack>
  )
}
