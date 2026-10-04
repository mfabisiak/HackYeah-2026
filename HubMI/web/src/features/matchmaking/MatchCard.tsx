import {
  Anchor,
  Badge,
  Button,
  Card,
  Group,
  List,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconArrowRight,
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
      return <IconCheck size={18} aria-hidden="true" />
    case 'TESTED':
      return <IconFlask size={18} aria-hidden="true" />
    case 'PILOT':
      return <IconLayersLinked size={18} aria-hidden="true" />
    default:
      return <IconBulb size={18} aria-hidden="true" />
  }
}

export function MatchCard({ match }: MatchCardProps) {
  const { innovation, score, reasons, matchedTerms } = match
  const quality = getMatchQualityLabel(score)
  const stageName = INNOVATION_STAGE_NAMES[innovation.stage] ?? innovation.stage

  return (
    <Card
      withBorder
      padding="xl"
      radius="lg"
      shadow="sm"
      component="article"
      aria-labelledby={`inno-title-${innovation.id}`}
      style={{
        borderLeftWidth: 6,
        borderLeftColor: `var(--mantine-color-${quality.color}-filled)`,
      }}
    >
      <Stack gap="md">
        <Group justify="space-between" align="center" wrap="wrap" gap="sm">
          <Badge
            color={quality.color}
            variant="filled"
            size="xl"
            radius="md"
            autoContrast
            leftSection={<IconSparkles size={18} aria-hidden="true" />}
            style={{
              height: 38,
              display: 'inline-flex',
              alignItems: 'center',
            }}
            styles={{
              label: { fontSize: '0.95rem', fontWeight: 700 },
            }}
          >
            {quality.label}
          </Badge>

          <Badge
            variant="outline"
            color="gray"
            size="xl"
            radius="md"
            leftSection={getStageIcon(innovation.stage)}
            style={{
              height: 38,
              borderWidth: 2,
              borderColor: 'var(--mantine-color-gray-6)',
              display: 'inline-flex',
              alignItems: 'center',
            }}
            styles={{
              label: { fontSize: '0.95rem', fontWeight: 600 },
            }}
          >
            {stageName}
          </Badge>
        </Group>

        <Title order={3} size="h3" id={`inno-title-${innovation.id}`} style={{ lineHeight: 1.3 }}>
          <Anchor
            component={Link}
            to={`/innowacje/${innovation.id}`}
            underline="hover"
            c="inherit"
            style={{ fontSize: '1.35rem', fontWeight: 700 }}
          >
            {innovation.title}
          </Anchor>
        </Title>

        <Text size="md" style={{ lineHeight: 1.6, fontSize: '1.05rem' }}>
          {innovation.summary}
        </Text>

        {reasons && reasons.length > 0 && (
          <Stack gap="xs" mt="xs">
            <Text size="md" fw={700}>
              Dlaczego to rozwiązanie pasuje do Twojej sytuacji:
            </Text>
            <List
              size="md"
              spacing="xs"
              icon={
                <ThemeIcon color={quality.color} size={24} radius="xl" variant="light" autoContrast>
                  <IconCircleCheck size={16} aria-hidden="true" />
                </ThemeIcon>
              }
              styles={{
                item: { lineHeight: 1.5, fontSize: '1.02rem' },
              }}
            >
              {reasons.map((reason, idx) => (
                <List.Item key={idx}>{reason}</List.Item>
              ))}
            </List>
          </Stack>
        )}

        {matchedTerms && matchedTerms.length > 0 && (
          <Group gap={8} align="center" mt="xs" wrap="wrap">
            <Text
              size="md"
              fw={600}
              c="light-dark(var(--mantine-color-gray-7), var(--mantine-color-dark-0))"
            >
              Dopasowane słowa z Twojego opisu:
            </Text>
            {matchedTerms.map((term, idx) => (
              <mark
                key={idx}
                style={{
                  backgroundColor: 'var(--mantine-color-yellow-light)',
                  color: 'light-dark(#664d03, var(--mantine-color-yellow-light-color))',
                  border: '1px solid var(--mantine-color-yellow-filled)',
                  padding: '4px 10px',
                  borderRadius: 6,
                  fontSize: '0.95rem',
                  fontWeight: 700,
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
          <Group gap="sm" mt="xs" wrap="wrap">
            {innovation.areas.map((area) => (
              <Badge
                key={area}
                variant="light"
                color="blue"
                size="xl"
                radius="md"
                style={{
                  height: 38,
                  border: '1.5px solid var(--mantine-color-blue-light-color)',
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
                styles={{
                  label: { fontSize: '1.05rem', fontWeight: 600 },
                }}
              >
                {SOCIAL_AREA_NAMES[area] ?? area}
              </Badge>
            ))}
          </Group>
        )}

        <Group justify="flex-start" mt="sm">
          <Button
            component={Link}
            to={`/innowacje/${innovation.id}`}
            variant="light"
            size="md"
            rightSection={<IconArrowRight size={18} aria-hidden="true" />}
          >
            Zobacz pełny opis tego rozwiązania
          </Button>
        </Group>
      </Stack>
    </Card>
  )
}
