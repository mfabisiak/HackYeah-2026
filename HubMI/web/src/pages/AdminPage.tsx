import { Alert, Anchor, Container, Paper, Stack, Text } from '@mantine/core'
import { IconShieldCheck } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { RequireRole } from '../auth/RequireRole'

function AdminContent() {
  return (
    <Container size="lg" p={0}>
      <PageHeader
        title="Panel administracyjny"
        subtitle="Zarządzanie systemem HubMI, uprawnieniami i moderacja treści."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Administracja' },
        ]}
      />

      <Stack gap="md">
        <Alert
          color="blue"
          icon={<IconShieldCheck size={20} aria-hidden="true" />}
          title="Strefa administracyjna chroniona rolą"
        >
          Masz dostęp do tej sekcji, ponieważ Twoje konto posiada uprawnienia administratora (rola{' '}
          <code>admin</code>).
        </Alert>

        <Paper withBorder p="lg" radius="md" component="nav" aria-label="Narzędzia administratora">
          <Stack gap="xs" component="ul" p={0} m={0} style={{ listStyle: 'none' }}>
            <li>
              <Anchor component={Link} to="/admin/plany" fw={600}>
                Plany adaptacji do przeglądu
              </Anchor>
              <Text size="sm" c="dimmed">
                Plany wdrożenia innowacji przygotowane przez asystenta AI (moduł 7), czekające na zatwierdzenie.
              </Text>
            </li>
          </Stack>
        </Paper>

        <Paper withBorder p="lg" radius="md">
          <Text size="sm">
            W tym miejscu w kolejnych modułach pojawią się narzędzia analityczne, trendy (moduł 6) oraz moderacja
            innowacji i wyzwań.
          </Text>
        </Paper>
      </Stack>
    </Container>
  )
}

export function AdminPage() {
  return (
    <RequireRole requiredRole="admin">
      <AdminContent />
    </RequireRole>
  )
}
