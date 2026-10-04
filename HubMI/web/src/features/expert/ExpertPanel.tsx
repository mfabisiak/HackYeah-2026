import {
  Alert,
  Card,
  Container,
  Group,
  SimpleGrid,
  Stack,
  Tabs,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconBulb,
  IconChecklist,
  IconInfoCircle,
  IconMessageDots,
  IconSparkles,
} from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { PageHeader } from '../../components/PageHeader'
import { ExpertAdvisoryHub } from './ExpertAdvisoryHub'
import { ExpertConsultationsQueue } from './ExpertConsultationsQueue'
import { ExpertIdeasReview } from './ExpertIdeasReview'
import { EXPERT_TAB_LABELS, type ExpertTab } from './constants'

export function ExpertPanel() {
  const [searchParams, setSearchParams] = useSearchParams()
  const rawTab = searchParams.get('tab')
  const activeTab = (rawTab && ['pomysly', 'konsultacje', 'doradztwo'].includes(rawTab) ? rawTab : 'pomysly') as ExpertTab

  // Metrics
  const [ideasCount, setIdeasCount] = useState<number | null>(null)
  const [threadsCount, setThreadsCount] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false

    const loadMetrics = async () => {
      try {
        const [ideasRes, threadsRes] = await Promise.allSettled([
          hubApi.admin.ideas(undefined, 0, 1),
          hubApi.threads.list(0, 1),
        ])

        if (cancelled) return

        if (ideasRes.status === 'fulfilled' && !ideasRes.value.error && ideasRes.value.value) {
          setIdeasCount(ideasRes.value.value.total)
        }
        if (threadsRes.status === 'fulfilled' && !threadsRes.value.error && threadsRes.value.value) {
          setThreadsCount(threadsRes.value.value.total)
        }
      } catch {
        // Ignore metrics failure
      }
    }

    void loadMetrics()

    return () => {
      cancelled = true
    }
  }, [])

  const handleTabChange = (val: string | null) => {
    if (!val) return
    setSearchParams({ tab: val })
  }

  return (
    <Container size="lg" py="xl">
      <Stack gap="xl">
        <PageHeader
          title="Strefa Eksperta Merytorycznego"
          subtitle="Merytoryczna ocena zgłaszanych innowacji społecznych, dialog z autorami oraz wsparcie doradcze dla małopolskich samorządów (JST)."
          breadcrumbs={[
            { title: 'Strona główna', href: '/' },
            { title: 'Strefa eksperta' },
          ]}
        />

        {/* Informational banner about expert scope vs ROPS administrative authority */}
        <Alert
          color="indigo"
          icon={<IconInfoCircle size={24} aria-hidden="true" />}
          title="Zakres uprawnień eksperta merytorycznego"
          radius="md"
          styles={{
            title: { fontSize: '1.15rem', fontWeight: 700 },
            message: { fontSize: '1rem', lineHeight: 1.5 },
          }}
        >
          W tej strefie analizujesz fiszki pomysłów i wnioski naborowe, przygotowujesz rekomendacje merytoryczne
          oraz prowadzisz dialog konsultacyjny z autorami i samorządami. Pamiętaj: wiążące decyzje formalne
          i grantowe podejmuje Regionalny Ośrodek Polityki Społecznej w Krakowie.
        </Alert>

        {/* 3 Metric Cards */}
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="lg">
          <Card
            withBorder
            padding="lg"
            radius="md"
            style={{
              cursor: 'pointer',
              borderLeft: '5px solid var(--mantine-color-blue-filled)',
              backgroundColor: activeTab === 'pomysly' ? 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))' : undefined,
            }}
            onClick={() => handleTabChange('pomysly')}
          >
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase">
                  Pomysły w bazie
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.4rem', fontWeight: 800 }}>
                  {ideasCount ?? '—'}
                </Title>
              </div>
              <ThemeIcon size={48} radius="md" color="blue" variant="light">
                <IconBulb size={26} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" mt="xs">
              Zgłoszone innowacje i wnioski mieszkańców Małopolski do analizy.
            </Text>
          </Card>

          <Card
            withBorder
            padding="lg"
            radius="md"
            style={{
              cursor: 'pointer',
              borderLeft: '5px solid var(--mantine-color-indigo-filled)',
              backgroundColor: activeTab === 'konsultacje' ? 'light-dark(var(--mantine-color-indigo-0), var(--mantine-color-dark-6))' : undefined,
            }}
            onClick={() => handleTabChange('konsultacje')}
          >
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase">
                  Wątki konsultacyjne
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.4rem', fontWeight: 800 }}>
                  {threadsCount ?? '—'}
                </Title>
              </div>
              <ThemeIcon size={48} radius="md" color="indigo" variant="light">
                <IconMessageDots size={26} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" mt="xs">
              Aktywny dialog z innowatorami i przedstawicielami gmin.
            </Text>
          </Card>

          <Card
            withBorder
            padding="lg"
            radius="md"
            style={{
              cursor: 'pointer',
              borderLeft: '5px solid var(--mantine-color-teal-filled)',
              backgroundColor: activeTab === 'doradztwo' ? 'light-dark(var(--mantine-color-teal-0), var(--mantine-color-dark-6))' : undefined,
            }}
            onClick={() => handleTabChange('doradztwo')}
          >
            <Group justify="space-between" align="flex-start">
              <div style={{ width: '100%' }}>
                <Text size="sm" c="dimmed" fw={600} tt="uppercase">
                  Doradztwo dla JST
                </Text>
                <Title order={2} size="h1" style={{ fontSize: '2.4rem', fontWeight: 800 }}>
                  Standard
                </Title>
              </div>
              <ThemeIcon size={48} radius="md" color="teal" variant="light">
                <IconSparkles size={26} aria-hidden="true" />
              </ThemeIcon>
            </Group>
            <Text size="sm" c="dimmed" mt="xs">
              Metodyka doboru i adaptacji innowacji w małopolskich gminach.
            </Text>
          </Card>
        </SimpleGrid>

        {/* Tab Navigation */}
        <Tabs value={activeTab} onChange={handleTabChange} radius="md">
          <Tabs.List mb="xl">
            <Tabs.Tab
              value="pomysly"
              leftSection={<IconBulb size={18} aria-hidden="true" />}
              style={{ fontSize: '1.05rem', fontWeight: 600, minHeight: 48 }}
            >
              {EXPERT_TAB_LABELS.pomysly}
            </Tabs.Tab>
            <Tabs.Tab
              value="konsultacje"
              leftSection={<IconMessageDots size={18} aria-hidden="true" />}
              style={{ fontSize: '1.05rem', fontWeight: 600, minHeight: 48 }}
            >
              {EXPERT_TAB_LABELS.konsultacje}
            </Tabs.Tab>
            <Tabs.Tab
              value="doradztwo"
              leftSection={<IconChecklist size={18} aria-hidden="true" />}
              style={{ fontSize: '1.05rem', fontWeight: 600, minHeight: 48 }}
            >
              {EXPERT_TAB_LABELS.doradztwo}
            </Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="pomysly">
            <ExpertIdeasReview />
          </Tabs.Panel>

          <Tabs.Panel value="konsultacje">
            <ExpertConsultationsQueue />
          </Tabs.Panel>

          <Tabs.Panel value="doradztwo">
            <ExpertAdvisoryHub />
          </Tabs.Panel>
        </Tabs>
      </Stack>
    </Container>
  )
}
