import { Badge, Card, Container, Divider, Group, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { MatchmakingView } from '../features/matchmaking/MatchmakingView'

const modules = [
  {
    title: 'Zasobnik wiedzy',
    text: 'Biblioteka innowacji społecznych, katalog wyzwań Małopolski oraz materiały i raporty edukacyjne.',
    soon: true,
  },
  {
    title: 'Kreator pomysłów',
    text: 'Zgłoś własny pomysł na innowację społeczną w formie zwięzłej fiszki i ubiegaj się o mikrogrant.',
    soon: true,
  },
]

export function HomePage() {
  return (
    <Stack gap="xl">
      <MatchmakingView />

      <Divider my="xl" label="Pozostałe obszary platformy" labelPosition="center" />

      <Container size="md" p={0}>
        <Stack gap="md">
          <Title order={2} size="h3">
            Odkrywaj HubMI
          </Title>
          <SimpleGrid
            cols={{ base: 1, sm: 2 }}
            component="ul"
            p={0}
            m={0}
            style={{ listStyle: 'none' }}
          >
            {modules.map((module) => (
              <li key={module.title}>
                <Card withBorder padding="lg" h="100%">
                  <Stack gap="sm">
                    <Group justify="space-between" wrap="nowrap" align="flex-start">
                      <Title order={3} size="h4">
                        {module.title}
                      </Title>
                      {module.soon && <Badge variant="light">W kolejnych modułach</Badge>}
                    </Group>
                    <Text size="sm" c="dimmed">
                      {module.text}
                    </Text>
                  </Stack>
                </Card>
              </li>
            ))}
          </SimpleGrid>
        </Stack>
      </Container>
    </Stack>
  )
}
