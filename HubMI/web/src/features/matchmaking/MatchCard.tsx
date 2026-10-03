import {
  Anchor,
  Badge,
  Card,
  Group,
  List,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconBulb,
  IconCheck,
  IconCircleCheck,
  IconFlask,
  IconLayersLinked,
  IconSparkles,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import {
  getMatchQualityLabel,
  INNOVATION_STAGE_NAMES,
  SOCIAL_AREA_NAMES,
} from './constants'
import type { MatchItem } from './types'

export interface MatchCardProps {
  match: MatchItem
}

function getStageIcon(stage: string) {
  switch (stage) {
    case 'IMPLEMENTED':
      return <IconCheck size={14} aria-hidden="true" />
    case 'TESTED':
      return <IconFlask size={14} aria-hidden="true" />
    case 'PILOT':
      return <IconLayersLinked size={14} aria-hidden="true" />
    default:
      return <IconBulb size={14} aria-hidden="true" />
  }
}

export function MatchCard({ match }: MatchCardProps) {
  const { innovation, score, reasons, matchedTerms } = match
  const quality = getMatchQualityLabel(score)
  const stageName = INNOVATION_STAGE_NAMES[innovation.stage] ?? innovation.stage

  return (
    <Card
      withBorder
      padding="lg"
      radius="md"
      shadow="xs"
      component="article"
      aria-labelledby={`inno-title-${innovation.id}`}
      style={{
        borderLeftWidth: 4,
        borderLeftColor: `var(--mantine-color-${quality.color}-filled)`,
      }}
    >
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" wrap="wrap" gap="xs">
          <Badge
            color={quality.color}
            variant="light"
            size="md"
            radius="sm"
            leftSection={<IconSparkles size={14} aria-hidden="true" />}
          >
            {quality.label}
          </Badge>

          <Badge
            variant="outline"
            color="gray"
            size="sm"
            leftSection={getStageIcon(innovation.stage)}
          >
            {stageName}
          </Badge>
        </Group>

        <Title order={3} size="h3" id={`inno-title-${innovation.id}`}>
          <Anchor
            component={Link}
            to={`/innowacje/${innovation.id}`}
            underline="hover"
            c="inherit"
          >
            {innovation.title}
          </Anchor>
        </Title>

        <Text size="sm" c="dimmed">
          {innovation.summary}
        </Text>

        {reasons && reasons.length > 0 && (
          <Stack gap={4} mt="xs">
            <Text size="xs" fw={700} c="dark.3">
              Dlaczego to rozwiązanie pasuje:
            </Text>
            <List
              size="sm"
              spacing={4}
              icon={
                <ThemeIcon color={quality.color} size={18} radius="xl" variant="light">
                  <IconCircleCheck size={12} aria-hidden="true" />
                </ThemeIcon>
              }
            >
              {reasons.map((reason, idx) => (
                <List.Item key={idx}>{reason}</List.Item>
              ))}
            </List>
          </Stack>
        )}

        {matchedTerms && matchedTerms.length > 0 && (
          <Group gap={6} align="center" mt="xs" wrap="wrap">
            <Text size="xs" c="dimmed">
              Dopasowane słowa:
            </Text>
            {matchedTerms.map((term, idx) => (
              <mark
                key={idx}
                style={{
                  backgroundColor: 'var(--mantine-color-yellow-1)',
                  color: 'var(--mantine-color-yellow-9)',
                  padding: '2px 6px',
                  borderRadius: 4,
                  fontSize: '0.75rem',
                  fontWeight: 500,
                }}
              >
                <span
                  style={{
                    position: 'absolute',
                    width: 1,
                    height: 1,
                    padding: 0,
                    margin: -1,
                    overflow: 'hidden',
                    clip: 'rect(0, 0, 0, 0)',
                    whiteSpace: 'nowrap',
                    border: 0,
                  }}
                >
                  Dopasowane słowo:{' '}
                </span>
                {term}
              </mark>
            ))}
          </Group>
        )}

        {innovation.areas && innovation.areas.length > 0 && (
          <Group gap="xs" mt="xs" wrap="wrap">
            {innovation.areas.map((area) => (
              <Badge key={area} variant="dot" color="blue" size="xs">
                {SOCIAL_AREA_NAMES[area] ?? area}
              </Badge>
            ))}
          </Group>
        )}
      </Stack>
    </Card>
  )
}
