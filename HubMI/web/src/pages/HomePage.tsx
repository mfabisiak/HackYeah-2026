import {
  Badge,
  Button,
  Card,
  Container,
  Divider,
  Group,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { IconArrowRight } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { MatchmakingView } from '../features/matchmaking/MatchmakingView'

const modules = [
  {
    title: 'Baza innowacji społecznych',
    text: 'Przeglądaj przetestowane i działające rozwiązania dla osób starszych i mieszkańców Małopolski.',
    to: '/innowacje',
    badge: 'Dostępne teraz',
    badgeColor: 'teal',
  },
  {
    title: 'Katalog wyzwań Małopolski',
    text: 'Zidentyfikowane potrzeby i wyzwania regionalne zebrane z małopolskich gmin i sołectw.',
    to: '/wyzwania',
    badge: 'Dostępne teraz',
    badgeColor: 'teal',
  },
  {
    title: 'Materiały i publikacje',
    text: 'Praktyczne poradniki krok po kroku, szablony Canva, instruktaże wideo oraz raporty z badań.',
    to: '/materialy',
    badge: 'Dostępne teraz',
    badgeColor: 'teal',
  },
  {
    title: 'Kreator pomysłów na innowacje',
    text: 'Zgłoś własny pomysł na innowację społeczną w formie zwięzłej fiszki i ubiegaj się o mikrogrant ROPS.',
    badge: 'W kolejnym module',
    badgeColor: 'gray',
  },
]

export function HomePage() {
  return (
    <Stack gap="xl">
      <MatchmakingView />

      <Divider
        my="3rem"
        label={
          <Text size="xl" fw={700} px="md">
            Pozostałe obszary platformy
          </Text>
        }
        labelPosition="center"
      />

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
                  <Stack gap="sm" justify="space-between" h="100%">
                    <Stack gap="xs">
                      <Group justify="space-between" wrap="nowrap" align="flex-start">
                        <Title order={3} size="h4">
                          {module.title}
                        </Title>
                        <Badge variant="light" color={module.badgeColor}>
                          {module.badge}
                        </Badge>
                      </Group>
                      <Text
                        size="md"
                        c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
                        style={{ lineHeight: 1.5 }}
                      >
                        {module.text}
                      </Text>
                    </Stack>

                    {module.to && (
                      <Group justify="flex-start" mt="xs">
                        <Button
                          component={Link}
                          to={module.to}
                          variant="light"
                          size="md"
                          rightSection={<IconArrowRight size={18} aria-hidden="true" />}
                        >
                          Przejdź do modułu
                        </Button>
                      </Group>
                    )}
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
