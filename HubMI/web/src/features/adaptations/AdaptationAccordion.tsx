import type { ReactNode } from 'react'
import { Accordion, Group, Stack, Text } from '@mantine/core'
import type { AdaptationJs } from 'hubmi-client'
import { AdaptationPlanView } from './AdaptationPlanView'
import { AdaptationStatusBadge } from './AdaptationStatusBadge'

export interface AdaptationAccordionProps {
  items: AdaptationJs[]
  /** Extra content under the plan of one item, e.g. the admin's review buttons. */
  actionsFor?: (adaptation: AdaptationJs) => ReactNode
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pl-PL', { day: 'numeric', month: 'long', year: 'numeric' })
}

/** A list of plans, each folded to its innovation, status and date, and unfolded to the whole plan. */
export function AdaptationAccordion({ items, actionsFor }: AdaptationAccordionProps) {
  return (
    <Accordion variant="separated" radius="md" chevronPosition="right">
      {items.map((adaptation) => (
        <Accordion.Item key={adaptation.id} value={adaptation.id}>
          <Accordion.Control>
            <Stack gap={4}>
              <Text fw={700} size="lg">
                {adaptation.innovationTitle}
              </Text>
              <Group gap="sm" wrap="wrap">
                <AdaptationStatusBadge status={adaptation.status} />
                <Text size="sm" c="dimmed">
                  Przygotowany <time dateTime={adaptation.createdAt}>{formatDate(adaptation.createdAt)}</time>
                </Text>
              </Group>
            </Stack>
          </Accordion.Control>
          <Accordion.Panel>
            <Stack gap="lg">
              <AdaptationPlanView adaptation={adaptation} />
              {actionsFor?.(adaptation)}
            </Stack>
          </Accordion.Panel>
        </Accordion.Item>
      ))}
    </Accordion>
  )
}
