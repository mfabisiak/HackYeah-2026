import {
  Alert,
  Container,
  Stack,
  Tabs,
} from '@mantine/core'
import {
  IconAlertCircle,
  IconBulb,
  IconDatabase,
  IconHeartHandshake,
  IconMessageHeart,
  IconShieldCheck,
} from '@tabler/icons-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '../../components/PageHeader'
import { AdminSummaryOverview } from './AdminSummaryOverview'
import { ContentManagement } from './ContentManagement'
import { FeedbackModerationQueue } from './FeedbackModerationQueue'
import { IdeasModerationQueue } from './IdeasModerationQueue'
import { TestRequestsModerationQueue } from './TestRequestsModerationQueue'

export function AdminPanel() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialTab = searchParams.get('tab') || 'wymaga-uwagi'
  const [activeTab, setActiveTab] = useState<string | null>(initialTab)

  const handleTabChange = (val: string | null) => {
    if (val) {
      setActiveTab(val)
      setSearchParams({ tab: val })
    }
  }

  return (
    <Container size="lg" p={0}>
      <Stack gap="xl">
        <PageHeader
          title="Panel administratora ROPS"
          subtitle="Zarządzanie systemem HubMI, moderacja pomysłów mieszkańców i nadzór nad wiedzą."
          breadcrumbs={[
            { title: 'Strona główna', href: '/' },
            { title: 'Panel administratora' },
          ]}
        />

        {/* Prominent visual banner indicating administrator context */}
        <Alert
          color="blue"
          icon={<IconShieldCheck size={24} aria-hidden="true" />}
          title="Strefa administratora – Regionalny Ośrodek Polityki Społecznej"
          radius="md"
          styles={{
            title: { fontSize: '1.2rem', fontWeight: 700 },
            message: { fontSize: '1.05rem', lineHeight: 1.5 },
          }}
        >
          Jesteś zalogowany z uprawnieniami pracownika ROPS (rola <code>admin</code>). Wszystkie
          decyzje o zmianie statusów i publikacji treści są rejestrowane w dzienniku audytu.
        </Alert>

        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant="outline"
          radius="md"
          styles={{
            tab: {
              fontSize: '1.05rem',
              fontWeight: 600,
              padding: '12px 18px',
            },
            tabLabel: { fontSize: '1.05rem' },
          }}
        >
          <Tabs.List>
            <Tabs.Tab
              value="wymaga-uwagi"
              leftSection={<IconAlertCircle size={20} aria-hidden="true" />}
            >
              Wymaga uwagi
            </Tabs.Tab>

            <Tabs.Tab
              value="pomysly"
              leftSection={<IconBulb size={20} aria-hidden="true" />}
            >
              Kolejka pomysłów
            </Tabs.Tab>

            <Tabs.Tab
              value="testy"
              leftSection={<IconHeartHandshake size={20} aria-hidden="true" />}
            >
              Zgłoszenia do testów
            </Tabs.Tab>

            <Tabs.Tab
              value="tresci"
              leftSection={<IconDatabase size={20} aria-hidden="true" />}
            >
              Zarządzanie treścią
            </Tabs.Tab>

            <Tabs.Tab
              value="opinie"
              leftSection={<IconMessageHeart size={20} aria-hidden="true" />}
            >
              Opinie testerów
            </Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="wymaga-uwagi" pt="xl">
            <AdminSummaryOverview onNavigateTab={handleTabChange} />
          </Tabs.Panel>

          <Tabs.Panel value="pomysly" pt="xl">
            <IdeasModerationQueue />
          </Tabs.Panel>

          <Tabs.Panel value="testy" pt="xl">
            <TestRequestsModerationQueue />
          </Tabs.Panel>

          <Tabs.Panel value="tresci" pt="xl">
            <ContentManagement />
          </Tabs.Panel>

          <Tabs.Panel value="opinie" pt="xl">
            <FeedbackModerationQueue />
          </Tabs.Panel>
        </Tabs>
      </Stack>
    </Container>
  )
}
