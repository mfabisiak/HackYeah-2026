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
  IconSparkles,
  IconShieldCheck,
  IconTrendingUp,
} from '@tabler/icons-react'
import { useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '../../components/PageHeader'
import { AdaptationsReviewQueue } from '../adaptations/AdaptationsReviewQueue'
import { AdminSummaryOverview } from './AdminSummaryOverview'
import { ContentManagement } from './ContentManagement'
import { FeedbackModerationQueue } from './FeedbackModerationQueue'
import { IdeasModerationQueue } from './IdeasModerationQueue'
import { TestRequestsModerationQueue } from './TestRequestsModerationQueue'
import { TrendsDashboard } from './TrendsDashboard'

/** `admin` sees everything; `expert` is an official without content management (the panel is otherwise the same). */
export type PanelMode = 'admin' | 'expert'

const PANEL_TEXT: Record<
  PanelMode,
  { title: string; subtitle: string; breadcrumb: string; bannerTitle: string; banner: ReactNode }
> = {
  admin: {
    title: 'Panel administratora ROPS',
    subtitle: 'Zarządzanie systemem HubMI, moderacja pomysłów mieszkańców i nadzór nad wiedzą.',
    breadcrumb: 'Panel administratora',
    bannerTitle: 'Strefa administratora – Regionalny Ośrodek Polityki Społecznej',
    banner: (
      <>
        Jesteś zalogowany z uprawnieniami pracownika ROPS (rola <code>admin</code>). Wszystkie decyzje o zmianie
        statusów i publikacji treści są rejestrowane w dzienniku audytu.
      </>
    ),
  },
  expert: {
    title: 'Panel eksperta ROPS',
    subtitle: 'Odpowiedzi dla mieszkańców w imieniu ROPS oraz ocena pomysłów, zgłoszeń do testów i planów adaptacji.',
    breadcrumb: 'Panel eksperta',
    bannerTitle: 'Strefa pracownika – Regionalny Ośrodek Polityki Społecznej',
    banner: (
      <>
        Jesteś zalogowany jako ekspert ROPS (rola <code>expert</code>). Odpowiadasz mieszkańcom w imieniu ROPS, a
        autorzy widzą Twoje imię i nazwisko. Zarządzanie treścią i trendy zostają po stronie administratora.
      </>
    ),
  },
}

const SUBTAB_TO_MAIN: Record<string, 'tresci'> = {
  innowacje: 'tresci',
  wyzwania: 'tresci',
  materialy: 'tresci',
}

export function AdminPanel({ mode = 'admin' }: { mode?: PanelMode }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const rawTab = searchParams.get('tab') || 'wymaga-uwagi'
  // Trends and content management are for the admin only.
  const isAdmin = mode === 'admin'
  const text = PANEL_TEXT[mode]

  const requestedMainTab = rawTab in SUBTAB_TO_MAIN ? 'tresci' : rawTab
  const resolvedMainTab =
    !isAdmin && (requestedMainTab === 'tresci' || requestedMainTab === 'trendy') ? 'wymaga-uwagi' : requestedMainTab
  const resolvedSubTab =
    rawTab in SUBTAB_TO_MAIN ? (rawTab as 'innowacje' | 'wyzwania' | 'materialy') : 'innowacje'

  const [activeTab, setActiveTab] = useState<string | null>(resolvedMainTab)
  const [contentSubTab, setContentSubTab] = useState<'innowacje' | 'wyzwania' | 'materialy'>(resolvedSubTab)

  const handleTabChange = (val: string | null) => {
    if (val) {
      if (!isAdmin && (val in SUBTAB_TO_MAIN || val === 'trendy')) return
      if (val in SUBTAB_TO_MAIN) {
        setActiveTab('tresci')
        setContentSubTab(val as 'innowacje' | 'wyzwania' | 'materialy')
        setSearchParams({ tab: val })
      } else {
        setActiveTab(val)
        setSearchParams({ tab: val })
      }
    }
  }

  return (
    <Container size="lg" p={0}>
      <Stack gap="xl">
        <PageHeader
          title={text.title}
          subtitle={text.subtitle}
          breadcrumbs={[
            { title: 'Strona główna', href: '/' },
            { title: text.breadcrumb },
          ]}
        />

        {/* Prominent visual banner indicating administrator context */}
        <Alert
          color="blue"
          icon={<IconShieldCheck size={24} aria-hidden="true" />}
          title={text.bannerTitle}
          radius="md"
          styles={{
            title: { fontSize: '1.2rem', fontWeight: 700 },
            message: { fontSize: '1.05rem', lineHeight: 1.5 },
          }}
        >
          {text.banner}
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
          <Tabs.List style={{ flexWrap: 'wrap' }}>
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

            {isAdmin && (
              <Tabs.Tab
                value="tresci"
                leftSection={<IconDatabase size={20} aria-hidden="true" />}
              >
                Zarządzanie treścią
              </Tabs.Tab>
            )}

            <Tabs.Tab
              value="opinie"
              leftSection={<IconMessageHeart size={20} aria-hidden="true" />}
            >
              Opinie testerów
            </Tabs.Tab>

            <Tabs.Tab
              value="plany"
              leftSection={<IconSparkles size={20} aria-hidden="true" />}
            >
              Plany adaptacji
            </Tabs.Tab>

            {isAdmin && (
              <Tabs.Tab
                value="trendy"
                leftSection={<IconTrendingUp size={20} aria-hidden="true" />}
              >
                Trendy i diagnoza
              </Tabs.Tab>
            )}
          </Tabs.List>

          <Tabs.Panel value="wymaga-uwagi" pt="xl">
            <AdminSummaryOverview onNavigateTab={handleTabChange} canSeeTrends={isAdmin} />
          </Tabs.Panel>

          <Tabs.Panel value="pomysly" pt="xl">
            <IdeasModerationQueue />
          </Tabs.Panel>

          <Tabs.Panel value="testy" pt="xl">
            <TestRequestsModerationQueue />
          </Tabs.Panel>

          {isAdmin && (
            <Tabs.Panel value="tresci" pt="xl">
              <ContentManagement key={contentSubTab} initialSubTab={contentSubTab} />
            </Tabs.Panel>
          )}

          <Tabs.Panel value="opinie" pt="xl">
            <FeedbackModerationQueue />
          </Tabs.Panel>

          <Tabs.Panel value="plany" pt="xl">
            <AdaptationsReviewQueue />
          </Tabs.Panel>

          {isAdmin && (
            <Tabs.Panel value="trendy" pt="xl">
              <TrendsDashboard
                onNavigateTab={(tab, subTab) => {
                  if (subTab) {
                    setActiveTab(tab)
                    setContentSubTab(subTab as 'innowacje' | 'wyzwania' | 'materialy')
                    setSearchParams({ tab: subTab })
                  } else {
                    handleTabChange(tab)
                  }
                }}
              />
            </Tabs.Panel>
          )}
        </Tabs>
      </Stack>
    </Container>
  )
}
