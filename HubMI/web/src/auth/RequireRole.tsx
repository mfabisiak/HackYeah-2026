import type { ReactNode } from 'react'
import { Button, Center, Container, Paper, Stack, Text, Title } from '@mantine/core'
import { IconShieldLock } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { RequireAuth } from './RequireAuth'

export interface RequireRoleProps {
  requiredRole: string | string[]
  children: ReactNode
  fallback?: ReactNode
}

function RoleCheck({
  requiredRole,
  children,
  fallback,
}: {
  requiredRole: string | string[]
  children: ReactNode
  fallback?: ReactNode
}) {
  const { hasRole, username } = useAuth()

  const requiredRoles = Array.isArray(requiredRole) ? requiredRole : [requiredRole]
  const isAuthorized = requiredRoles.some((r) => hasRole(r))

  if (!isAuthorized) {
    if (fallback) {
      return <>{fallback}</>
    }

    return (
      <Container size="sm" py="xl">
        <Paper
          withBorder
          p="xl"
          radius="md"
          shadow="sm"
          role="region"
          aria-label="Brak uprawnień"
          data-testid="forbidden-page"
        >
          <Stack align="center" gap="md">
            <Center
              style={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                backgroundColor: 'var(--mantine-color-red-light)',
                color: 'var(--mantine-color-red-filled)',
              }}
              aria-hidden="true"
            >
              <IconShieldLock size={32} />
            </Center>
            <Title order={1} size="h3" ta="center" c="red.7">
              Brak uprawnień (403)
            </Title>
            <Text c="dimmed" size="sm" ta="center" maw={440}>
              Konto {username ? <strong>{username}</strong> : 'użytkownika'} nie posiada wymaganej roli (
              <code>{requiredRoles.join(', ')}</code>) do przeglądania tej zawartości.
            </Text>
            <Button component={Link} to="/" variant="light" mt="xs">
              Powrót do strony głównej
            </Button>
          </Stack>
        </Paper>
      </Container>
    )
  }

  return <>{children}</>
}

export function RequireRole({ requiredRole, children, fallback }: RequireRoleProps) {
  return (
    <RequireAuth>
      <RoleCheck requiredRole={requiredRole} fallback={fallback}>
        {children}
      </RoleCheck>
    </RequireAuth>
  )
}
