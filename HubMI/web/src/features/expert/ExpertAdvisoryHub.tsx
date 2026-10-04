import {
  Alert,
  Button,
  Card,
  Group,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconArrowRight,
  IconChecklist,
  IconFileSpreadsheet,
  IconMapPin,
  IconSearch,
  IconSparkles,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'

export function ExpertAdvisoryHub() {
  return (
    <Stack gap="xl">
      <Alert
        color="teal"
        icon={<IconSparkles size={24} aria-hidden="true" />}
        title="Rola doradcza dla małopolskich samorządów (JST)"
        radius="md"
        styles={{
          title: { fontSize: '1.2rem', fontWeight: 700 },
          message: { fontSize: '1rem', lineHeight: 1.6 },
        }}
      >
        Ekspert merytoryczny wspiera gminy i powiaty w diagnozie problemów seniorów oraz w wyborze
        gotowych, przetestowanych innowacji społecznych z bazy ROPS do wdrożenia w lokalnych politykach społecznych.
      </Alert>

      {/* 3 Action Pillars */}
      <SimpleGrid cols={{ base: 1, md: 3 }} spacing="lg">
        <Card withBorder padding="lg" radius="md">
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Stack gap="xs">
              <ThemeIcon size={48} radius="md" color="teal" variant="light">
                <IconMapPin size={26} aria-hidden="true" />
              </ThemeIcon>
              <Title order={3} size="h3">
                Wyzwania Małopolski
              </Title>
              <Text size="sm" c="dimmed">
                Baza zdiagnozowanych potrzeb i wyzwań społecznych zgłaszanych przez gminy i powiaty województwa małopolskiego.
              </Text>
            </Stack>
            <Button
              component={Link}
              to="/wyzwania"
              variant="light"
              color="teal"
              rightSection={<IconArrowRight size={16} aria-hidden="true" />}
            >
              Przeglądaj wyzwania gmin
            </Button>
          </Stack>
        </Card>

        <Card withBorder padding="lg" radius="md">
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Stack gap="xs">
              <ThemeIcon size={48} radius="md" color="blue" variant="light">
                <IconFileSpreadsheet size={26} aria-hidden="true" />
              </ThemeIcon>
              <Title order={3} size="h3">
                Katalog Innowacji ROPS
              </Title>
              <Text size="sm" c="dimmed">
                Baza sprawdzonych, przetestowanych rozwiązań i innowacji gotowych do adaptacji i wdrożenia w samorządach.
              </Text>
            </Stack>
            <Button
              component={Link}
              to="/innowacje"
              variant="light"
              color="blue"
              rightSection={<IconArrowRight size={16} aria-hidden="true" />}
            >
              Biblioteka innowacji
            </Button>
          </Stack>
        </Card>

        <Card withBorder padding="lg" radius="md">
          <Stack justify="space-between" style={{ height: '100%' }}>
            <Stack gap="xs">
              <ThemeIcon size={48} radius="md" color="grape" variant="light">
                <IconSearch size={26} aria-hidden="true" />
              </ThemeIcon>
              <Title order={3} size="h3">
                Asystent Dopasowania
              </Title>
              <Text size="sm" c="dimmed">
                Silnik matchmakingowy AI wyszukujący innowacje odpowiadające na specyficzny opis wyzwania przedstawiciela gminy.
              </Text>
            </Stack>
            <Button
              component={Link}
              to="/dopasuj"
              variant="light"
              color="grape"
              rightSection={<IconArrowRight size={16} aria-hidden="true" />}
            >
              Dopasuj innowację (AI)
            </Button>
          </Stack>
        </Card>
      </SimpleGrid>

      {/* Advisory Methodology Guide */}
      <Paper withBorder p="xl" radius="md">
        <Stack gap="md">
          <Group gap="xs">
            <ThemeIcon size={36} radius="md" color="indigo" variant="light">
              <IconChecklist size={20} aria-hidden="true" />
            </ThemeIcon>
            <Title order={3} size="h3">
              Standard wsparcia doradczego dla JST – 4 kroki adaptacji innowacji
            </Title>
          </Group>

          <Text size="sm" c="dimmed">
            Poniższy proces metodyczny służy ekspertom jako wytyczne podczas prowadzenia doradztwa dla samorządów planujących wdrożenie innowacji senioralnych.
          </Text>

          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg" mt="xs">
            <Paper withBorder p="md" radius="md">
              <Group align="flex-start" gap="sm">
                <ThemeIcon size={32} radius="xl" color="blue">
                  1
                </ThemeIcon>
                <Stack gap={4} style={{ flex: 1 }}>
                  <Text fw={700} size="md">
                    Diagnoza potrzeb seniorów w gminie
                  </Text>
                  <Text size="sm" c="dimmed">
                    Identyfikacja barier (np. wykluczenie transportowe na terenach wiejskich, samotność osób 75+, brak dostępności cyfrowej usług).
                  </Text>
                </Stack>
              </Group>
            </Paper>

            <Paper withBorder p="md" radius="md">
              <Group align="flex-start" gap="sm">
                <ThemeIcon size={32} radius="xl" color="teal">
                  2
                </ThemeIcon>
                <Stack gap={4} style={{ flex: 1 }}>
                  <Text fw={700} size="md">
                    Dobór gotowej innowacji z bazy
                  </Text>
                  <Text size="sm" c="dimmed">
                    Wybór przetestowanego modelu (np. Mobilny Asystent Seniora, Sąsiedzka Sieć Pomocy) i adaptacja do skali gminy.
                  </Text>
                </Stack>
              </Group>
            </Paper>

            <Paper withBorder p="md" radius="md">
              <Group align="flex-start" gap="sm">
                <ThemeIcon size={32} radius="xl" color="orange">
                  3
                </ThemeIcon>
                <Stack gap={4} style={{ flex: 1 }}>
                  <Text fw={700} size="md">
                    Montaż finansowy i nabory grantowe
                  </Text>
                  <Text size="sm" c="dimmed">
                    Wykorzystanie naborów grantowych ROPS (Kreator wniosków), środków własnych JST lub funduszy europejskich FAMI/EFS+.
                  </Text>
                </Stack>
              </Group>
            </Paper>

            <Paper withBorder p="md" radius="md">
              <Group align="flex-start" gap="sm">
                <ThemeIcon size={32} radius="xl" color="indigo">
                  4
                </ThemeIcon>
                <Stack gap={4} style={{ flex: 1 }}>
                  <Text fw={700} size="md">
                    Pilotaż i ewaluacja z testerami
                  </Text>
                  <Text size="sm" c="dimmed">
                    Zaangażowanie seniorów jako testerów innowacji przed stałym wpisaniem rozwiązania do Gminnego Programu Senioralnego.
                  </Text>
                </Stack>
              </Group>
            </Paper>
          </SimpleGrid>
        </Stack>
      </Paper>
    </Stack>
  )
}
