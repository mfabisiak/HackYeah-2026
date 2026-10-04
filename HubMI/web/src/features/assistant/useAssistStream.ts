import { useCallback, useEffect, useRef, useState } from 'react'
import {
  CreateIdeaJs,
  type AssistResponseJs,
  type FlowJs,
  type SimilarInnovationJs,
  type StreamJs,
  type SuggestionJs,
} from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { describeApiError } from '../../api/errors'

export interface AssistIdea {
  title: string
  essence: string
  targetGroups: string[]
  stage: string
}

export type AssistPhase = 'idle' | 'running' | 'stopped' | 'done' | 'failed'

export interface AssistState {
  phase: AssistPhase
  /** `AssistMode` name of the call that this state is about. */
  mode: string | null
  similar: SimilarInnovationJs[]
  noveltyHint: string | null
  suggestions: SuggestionJs[]
  flow: FlowJs | null
  aiStatus: string | null
  error: string | null
}

const IDLE: AssistState = {
  phase: 'idle',
  mode: null,
  similar: [],
  noveltyHint: null,
  suggestions: [],
  flow: null,
  aiStatus: null,
  error: null,
}

/**
 * Runs the assistant and keeps what it has said so far: the similar innovations at once, then each suggestion as the
 * model writes it. Only the latest call counts (starting another one cancels the previous), and leaving the page
 * cancels it too, which stops the generation on the server.
 */
export function useAssistStream() {
  const [state, setState] = useState<AssistState>(IDLE)
  const stream = useRef<StreamJs<AssistResponseJs> | null>(null)
  /** The call whose answers count; `0` when none does. */
  const active = useRef(0)
  const counter = useRef(0)

  const cancel = useCallback(() => {
    stream.current?.cancel()
    stream.current = null
    active.current = 0
  }, [])

  useEffect(() => cancel, [cancel])

  const run = useCallback(
    (idea: AssistIdea, mode: string) => {
      cancel()
      counter.current += 1
      const id = counter.current
      active.current = id
      const isCurrent = () => active.current === id

      setState({ ...IDLE, phase: 'running', mode })
      const call = hubApi.assistant.assistDraft(
        new CreateIdeaJs(idea.title, idea.essence, idea.targetGroups, idea.stage),
        mode,
        {
          onSimilar: (part) => {
            if (!isCurrent()) return
            setState((s) => ({ ...s, similar: Array.from(part.similar), noveltyHint: part.noveltyHint }))
          },
          onSuggestion: (suggestion) => {
            if (!isCurrent()) return
            setState((s) => ({ ...s, suggestions: [...s.suggestions, suggestion] }))
          },
          onFlow: (flow) => {
            if (!isCurrent()) return
            setState((s) => ({ ...s, flow }))
          },
        },
      )
      stream.current = call

      void call.result.then((result) => {
        if (!isCurrent()) return
        active.current = 0
        stream.current = null
        const answer = result.value
        if (answer) {
          setState((s) => ({
            ...s,
            phase: 'done',
            similar: Array.from(answer.similar),
            noveltyHint: answer.noveltyHint,
            suggestions: Array.from(answer.suggestions),
            flow: answer.flow ?? null,
            aiStatus: answer.aiStatus,
          }))
        } else {
          setState((s) => ({
            ...s,
            phase: 'failed',
            error: result.error ? describeApiError(result.error) : 'Nie udało się uzyskać odpowiedzi asystenta.',
          }))
        }
      })
    },
    [cancel],
  )

  /** The user's own "stop": keeps what has arrived and says that the answer is not complete. */
  const stop = useCallback(() => {
    cancel()
    setState((s) => (s.phase === 'running' ? { ...s, phase: 'stopped' } : s))
  }, [cancel])

  return { state, run, stop }
}
