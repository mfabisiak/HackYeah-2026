import { Badge, Card, Group, SimpleGrid, Stack, Text, Title } from '@mantine/core'

const modules = [
  { title: 'Matchmaking społeczny', text: 'Opisz problem swoimi słowami, a znajdziemy sprawdzone rozwiązania.', soon: true },
  { title: 'Zasobnik wiedzy', text: 'Biblioteka innowacji, wyzwania Małopolski i materiały edukacyjne.', soon: true },
  { title: 'Kreator pomysłów', text: 'Zgłoś własny pomysł na innowację społeczną w formie krótkiej fiszki.', soon: true },
]

export function HomePage() {
  return (
    <Stack gap="xl">
      <Stack gap="xs">
        <Title order={2}>Małopolski Hub Innowacji Społecznych</Title>
        <Text size="lg" maw={720}>
          Miejsce, w którym spotykają się potrzeby mieszkańców, wiedza ekspertów i sprawdzone rozwiązania.
        </Text>
      </Stack>
      <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} component="ul" p={0} m={0} style={{ listStyle: 'none' }}>
        {modules.map((module) => (
          <li key={module.title}>
            <Card withBorder padding="lg" h="100%">
              <Stack gap="sm">
                <Group justify="space-between" wrap="nowrap" align="flex-start">
                  <Title order={3} size="h4">
                    {module.title}
                  </Title>
                  {module.soon && <Badge variant="light">Wkrótce</Badge>}
                </Group>
                <Text>{module.text}</Text>
              </Stack>
            </Card>
          </li>
        ))}
      </SimpleGrid>
    </Stack>
  )
}
