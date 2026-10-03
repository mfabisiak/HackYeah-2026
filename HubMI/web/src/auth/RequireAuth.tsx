import type { ReactNode } from 'react'
import { Button, Center, Container, Paper, Stack, Text, Title } from '@mantine/core'
import { IconLock } from '@tabler/icons-react'
import { useAuth } from './AuthContext'
import { LoadingState } from '../components/LoadingState'

export interface RequireAuthProps {
  children: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const { ready, authenticated, login } = useAuth()

  if (!ready) {
    return <LoadingState message="Sprawdzanie uprawnień..." />
  }

  if (!authenticated) {
    return (
      <Container size="xs" py="xl">
        <Paper
          withBorder
          p="xl"
          radius="md"
          shadow="sm"
          role="region"
          aria-label="Wymagane logowanie"
          data-testid="require-auth-prompt"
        >
          <Stack align="center" gap="md">
            <Center
              style={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                backgroundColor: 'var(--mantine-color-blue-light)',
                color: 'var(--mantine-color-blue-filled)',
              }}
              aria-hidden="true"
            >
              <IconLock size={28} />
            </Center>
            <Title order={2} size="h3" ta="center">
              Wymagane logowanie
            </Title>
            <Text c="dimmed" size="sm" ta="center">
              Aby uzyskać dostęp do tej sekcji, musisz zalogować się do systemu HubMI.
            </Text>
            <Button onClick={login} fullWidth mt="xs">
              Zaloguj się
            </Button>
          </Stack>
        </Paper>
      </Container>
    )
  }

  return <>{children}</>
}
