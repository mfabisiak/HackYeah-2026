import { SegmentedControl, Stack } from '@mantine/core'
import { useState } from 'react'
import { ChallengesCrud } from './ChallengesCrud'
import { InnovationsCrud } from './InnovationsCrud'
import { MaterialsCrud } from './MaterialsCrud'

export interface ContentManagementProps {
  initialSubTab?: 'innowacje' | 'wyzwania' | 'materialy'
}

export function ContentManagement({ initialSubTab = 'innowacje' }: ContentManagementProps) {
  const [activeSubTab, setActiveSubTab] = useState<'innowacje' | 'wyzwania' | 'materialy'>(initialSubTab)

  return (
    <Stack gap="xl">
      <SegmentedControl
        value={activeSubTab}
        onChange={(val) => setActiveSubTab(val as 'innowacje' | 'wyzwania' | 'materialy')}
        data={[
          { label: 'Baza innowacji społecznych', value: 'innowacje' },
          { label: 'Katalog wyzwań Małopolski', value: 'wyzwania' },
          { label: 'Materiały i publikacje edukacyjne', value: 'materialy' },
        ]}
        size="md"
        styles={{
          root: { maxWidth: 650 },
          label: { fontSize: '1rem', fontWeight: 600, padding: '8px 16px' },
        }}
      />

      {activeSubTab === 'innowacje' && <InnovationsCrud />}
      {activeSubTab === 'wyzwania' && <ChallengesCrud />}
      {activeSubTab === 'materialy' && <MaterialsCrud />}
    </Stack>
  )
}
