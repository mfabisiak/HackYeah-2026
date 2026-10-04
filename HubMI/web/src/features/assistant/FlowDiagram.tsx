import { Badge, Card, Group, Stack, Text, ThemeIcon, Title, VisuallyHidden } from '@mantine/core'
import { IconArrowRight } from '@tabler/icons-react'
import type { FlowJs } from 'hubmi-client'

export interface FlowDiagramProps {
  flow: FlowJs
}

/**
 * The idea as a sequence of steps between its actors. The numbered list is the whole content (a screen reader reads
 * "senior do koordynator: zamawia przejazd"); the arrows are decoration.
 */
export function FlowDiagram({ flow }: FlowDiagramProps) {
  return (
    <Stack gap="md" component="section" aria-labelledby="flow-heading">
      <Title id="flow-heading" order={3} size="h4">
        Jak to ma działać
      </Title>

      <Group gap="xs" component="div" aria-label="Uczestnicy">
        <Text size="sm" c="dimmed">
          Uczestnicy:
        </Text>
        {Array.from(flow.actors).map((actor) => (
          <Badge key={actor} variant="outline" size="lg" tt="none">
            {actor}
          </Badge>
        ))}
      </Group>

      <Stack component="ol" gap="sm" p={0} m={0} style={{ listStyle: 'none' }}>
        {Array.from(flow.steps).map((step, index) => (
          <li key={`${index}-${step.label}`}>
            <Card withBorder padding="md" radius="md">
              <Group gap="md" wrap="nowrap" align="flex-start">
                <ThemeIcon variant="light" size="lg" radius="xl" aria-hidden="true">
                  {index + 1}
                </ThemeIcon>
                <Stack gap={4}>
                  <Group gap={8} wrap="wrap">
                    <Text fw={700} size="md">
                      {step.from}
                    </Text>
                    <IconArrowRight size={18} aria-hidden="true" />
                    <VisuallyHidden>do</VisuallyHidden>
                    <Text fw={700} size="md">
                      {step.to}
                    </Text>
                  </Group>
                  <Text size="md">{step.label}</Text>
                </Stack>
              </Group>
            </Card>
          </li>
        ))}
      </Stack>
    </Stack>
  )
}
