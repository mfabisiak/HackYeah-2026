import { Button, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { IconPlayerStop, IconSparkles } from '@tabler/icons-react'
import { AiNotice } from '../../components/AiNotice'
import { ErrorAlert } from '../../components/ErrorAlert'
import { AI_STATUS_NOTICES, ASSIST_MODE_NAMES, ASSIST_MODES } from './constants'
import { FlowDiagram } from './FlowDiagram'
import { SimilarInnovations } from './SimilarInnovations'
import { SuggestionList } from './SuggestionList'
import { useAssistStream, type AssistIdea } from './useAssistStream'

export interface AssistantPanelProps {
  /**
   * Called when the user picks what the assistant should do. Returns the idea to work on, or `null` when it is not
   * ready (the form then shows what is missing); this way the panel can sit under any form that describes an idea.
   */
  resolveIdea: () => AssistIdea | null
}

const WORKING: Record<string, string> = {
  SIMILAR: 'Szukam podobnych rozwiązań w bazie…',
  EXPAND: 'Asystent pisze podpowiedzi…',
  RISKS: 'Asystent szuka ryzyk…',
  FLOW: 'Asystent rozpisuje kroki…',
}

export function AssistantPanel({ resolveIdea }: AssistantPanelProps) {
  const { state, run, stop } = useAssistStream()
  const running = state.phase === 'running'

  const start = (mode: string) => {
    const idea = resolveIdea()
    if (idea) run(idea, mode)
  }

  const showsSuggestions = state.mode === 'EXPAND' || state.mode === 'RISKS'

  return (
    <Stack gap="lg" component="section" aria-labelledby="assistant-heading">
      <Stack gap={4}>
        <Group gap="xs">
          <IconSparkles size={26} color="var(--mantine-color-grape-filled)" aria-hidden="true" />
          <Title id="assistant-heading" order={2} size="h3">
            Asystent AI
          </Title>
        </Group>
        <Text size="md">Wybierz, w czym asystent ma Ci pomóc. Odpowiedź pojawia się na bieżąco, w miarę jak powstaje.</Text>
      </Stack>

      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
        {ASSIST_MODES.map((mode) => (
          <Button
            key={mode.value}
            variant={state.mode === mode.value ? 'filled' : 'light'}
            color="grape"
            size="lg"
            h="auto"
            py="sm"
            fullWidth
            justify="flex-start"
            onClick={() => start(mode.value)}
            styles={{ label: { whiteSpace: 'normal', textAlign: 'left' } }}
          >
            <Stack gap={2} align="flex-start">
              <Text fw={700} size="md" inherit>
                {mode.label}
              </Text>
              <Text size="sm" fw={400} inherit>
                {mode.description}
              </Text>
            </Stack>
          </Button>
        ))}
      </SimpleGrid>

      <Stack gap="lg" role="region" aria-label="Odpowiedź asystenta" aria-live="polite" aria-busy={running}>
        {running && (
          <Group gap="sm" data-testid="assistant-working">
            <Loader size="sm" aria-hidden="true" />
            <Text size="md">{WORKING[state.mode ?? ''] ?? 'Asystent pracuje…'}</Text>
            <Button size="xs" variant="default" leftSection={<IconPlayerStop size={14} aria-hidden="true" />} onClick={stop}>
              Przerwij
            </Button>
          </Group>
        )}

        {state.phase === 'stopped' && (
          <Text size="md" c="dimmed">
            Przerwano. Widzisz to, co zdążyło się pojawić.
          </Text>
        )}

        {state.mode && state.phase !== 'idle' && (
          <Title order={3} size="h4" c="dimmed" data-testid="assistant-mode">
            {ASSIST_MODE_NAMES[state.mode]}
          </Title>
        )}

        {state.phase === 'failed' && state.error && <ErrorAlert title="Nie udało się" message={state.error} />}

        {(state.noveltyHint || state.similar.length > 0) && (
          <SimilarInnovations similar={state.similar} noveltyHint={state.noveltyHint} />
        )}

        {state.aiStatus && AI_STATUS_NOTICES[state.aiStatus] && (
          <ErrorAlert title="Asystent AI nie odpowiedział" message={AI_STATUS_NOTICES[state.aiStatus]} />
        )}

        {showsSuggestions && state.suggestions.length > 0 && (
          <Stack gap="md">
            <AiNotice />
            <SuggestionList
              title={state.mode === 'RISKS' ? 'Ryzyka do sprawdzenia' : 'Jak rozwinąć pomysł'}
              suggestions={state.suggestions}
              similar={state.similar}
            />
          </Stack>
        )}

        {state.mode === 'FLOW' && state.flow && (
          <Stack gap="md">
            <AiNotice />
            <FlowDiagram flow={state.flow} />
          </Stack>
        )}
      </Stack>
    </Stack>
  )
}
