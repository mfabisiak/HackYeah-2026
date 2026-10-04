import { Alert, Card, Group, List, Stack, Text, ThemeIcon, Title } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'
import type { AdaptationJs, AdaptationStepJs } from 'hubmi-client'
import { AiNotice } from '../../components/AiNotice'
import { formatPln, INSTITUTION_TYPE_NAMES, monthLabel } from './constants'
import { AdaptationStatusBadge } from './AdaptationStatusBadge'

export interface AdaptationStepsProps {
  steps: AdaptationStepJs[]
}

/** The steps of a plan in the order they start; also shown on their own while a plan is still being written. */
export function AdaptationSteps({ steps }: AdaptationStepsProps) {
  return (
    <Stack component="ol" gap="sm" p={0} m={0} style={{ listStyle: 'none' }}>
      {steps.map((step, index) => (
        <li key={`${index}-${step.title}`}>
          <Card withBorder padding="md" radius="md">
            <Group gap="md" wrap="nowrap" align="flex-start">
              <ThemeIcon variant="light" size="lg" radius="xl" aria-hidden="true">
                {index + 1}
              </ThemeIcon>
              <Stack gap={2}>
                <Text fw={700} size="md">
                  {step.title}
                </Text>
                <Text size="sm" c="dimmed">
                  {monthLabel(step.startMonth)}
                </Text>
                <Text size="md">{step.description}</Text>
              </Stack>
            </Group>
          </Card>
        </li>
      ))}
    </Stack>
  )
}

export interface AdaptationPlanViewProps {
  adaptation: AdaptationJs
  /** Level of the plan's own heading; sections under it are one level deeper. */
  headingOrder?: 2 | 3
}

export function AdaptationPlanView({ adaptation, headingOrder = 3 }: AdaptationPlanViewProps) {
  const { plan, institution } = adaptation
  const sectionOrder = (headingOrder + 1) as 3 | 4

  return (
    <Stack gap="lg" data-testid="adaptation-plan">
      <Group gap="sm" wrap="wrap">
        <AdaptationStatusBadge status={adaptation.status} />
        <Text size="sm" c="dimmed">
          {INSTITUTION_TYPE_NAMES[institution.type] ?? institution.type}, {institution.staffCount} os., budżet{' '}
          {formatPln(institution.budgetPln)}
        </Text>
      </Group>

      <AiNotice>
        Ten plan napisał asystent AI (model: {adaptation.model}).{' '}
        {adaptation.status === 'APPROVED'
          ? 'Pracownik ROPS go przejrzał i zatwierdził.'
          : adaptation.status === 'REJECTED'
            ? 'Pracownik ROPS go przejrzał i odrzucił.'
            : 'Czeka na przegląd pracownika ROPS. Do tego czasu traktuj go jako propozycję.'}
      </AiNotice>

      {adaptation.adminComment && (
        <Alert color={adaptation.status === 'REJECTED' ? 'red' : 'teal'} variant="light" title="Komentarz ROPS">
          <Text size="md">{adaptation.adminComment}</Text>
        </Alert>
      )}

      <Stack gap={4}>
        <Title order={headingOrder} size="h4">
          Forma usługi
        </Title>
        <Text size="md">{plan.serviceForm}</Text>
      </Stack>

      <Stack gap="sm">
        <Title order={sectionOrder} size="h5">
          Kroki pilotażu
        </Title>
        <AdaptationSteps steps={Array.from(plan.steps)} />
      </Stack>

      <Group align="flex-start" gap="xl" grow>
        <Stack gap="xs">
          <Title order={sectionOrder} size="h5">
            Potrzebne zasoby
          </Title>
          <List size="md" spacing={4}>
            {Array.from(plan.requiredResources).map((resource) => (
              <List.Item key={resource}>{resource}</List.Item>
            ))}
          </List>
        </Stack>
        <Stack gap="xs">
          <Title order={sectionOrder} size="h5">
            Ryzyka
          </Title>
          <List size="md" spacing={4}>
            {Array.from(plan.risks).map((risk) => (
              <List.Item key={risk}>{risk}</List.Item>
            ))}
          </List>
        </Stack>
      </Group>

      <Stack gap="xs">
        <Title order={sectionOrder} size="h5">
          Szacowany koszt pilotażu
        </Title>
        <Text size="lg" fw={700}>
          {formatPln(plan.estimatedCostPln)}
        </Text>
        {plan.exceedsBudget && (
          <Alert
            color="orange"
            variant="light"
            icon={<IconAlertTriangle size={22} aria-hidden="true" />}
            title="Koszt przekracza budżet"
          >
            <Text size="md">
              Asystent oszacował koszt wyżej niż budżet instytucji ({formatPln(institution.budgetPln)}). Plan trzeba
              będzie odchudzić, np. zmniejszyć zakres albo wydłużyć pilotaż.
            </Text>
          </Alert>
        )}
      </Stack>
    </Stack>
  )
}
