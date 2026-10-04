import { Stack, Tabs } from '@mantine/core'
import { IconBulb, IconFileText, IconMapPin } from '@tabler/icons-react'
import { useState } from 'react'
import { ChallengesCrud } from './ChallengesCrud'
import { InnovationsCrud } from './InnovationsCrud'
import { MaterialsCrud } from './MaterialsCrud'

export interface ContentManagementProps {
  initialSubTab?: 'innowacje' | 'wyzwania' | 'materialy'
}

export function ContentManagement({ initialSubTab = 'innowacje' }: ContentManagementProps) {
  const [activeSubTab, setActiveSubTab] = useState<string>(initialSubTab)

  return (
    <Stack gap="xl">
      <Tabs
        value={activeSubTab}
        onChange={(val) => val && setActiveSubTab(val)}
        variant="pills"
        styles={{
          tab: {
            fontSize: '1.05rem',
            fontWeight: 600,
            padding: '10px 18px',
          },
        }}
      >
        <Tabs.List>
          <Tabs.Tab
            value="innowacje"
            leftSection={<IconBulb size={18} aria-hidden="true" />}
          >
            Innowacje społeczne
          </Tabs.Tab>
          <Tabs.Tab
            value="wyzwania"
            leftSection={<IconMapPin size={18} aria-hidden="true" />}
          >
            Wyzwania Małopolski
          </Tabs.Tab>
          <Tabs.Tab
            value="materialy"
            leftSection={<IconFileText size={18} aria-hidden="true" />}
          >
            Materiały i publikacje edukacyjne
          </Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="innowacje" pt="lg">
          <InnovationsCrud />
        </Tabs.Panel>
        <Tabs.Panel value="wyzwania" pt="lg">
          <ChallengesCrud />
        </Tabs.Panel>
        <Tabs.Panel value="materialy" pt="lg">
          <MaterialsCrud />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}

