import { useCallback, useEffect, useRef, useState } from 'react'
import {
  InstitutionJs,
  type AdaptationResponseJs,
  type AdaptationStepJs,
  type StreamJs,
} from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { describeApiError } from '../../api/errors'
import type { Institution } from './institutionValidation'

export type AdaptationPhase = 'idle' | 'running' | 'stopped' | 'done' | 'failed'

export interface AdaptationState {
  phase: AdaptationPhase
  /** Steps of the plan as the model writes them; the stored plan (in [response]) may differ a little. */
  steps: AdaptationStepJs[]
  response: AdaptationResponseJs | null
  error: string | null
  /** The call failed because the session is gone, so the user has to log in again. */
  unauthorized: boolean
}

const IDLE: AdaptationState = { phase: 'idle', steps: [], response: null, error: null, unauthorized: false }

/**
 * Asks the Middleman for a plan and keeps its steps as they arrive. Only the latest call counts, and leaving the page
 * cancels a running one, which stops the generation on the server.
 */
export function useAdaptationStream() {
  const [state, setState] = useState<AdaptationState>(IDLE)
  const stream = useRef<StreamJs<AdaptationResponseJs> | null>(null)
  const active = useRef(0)
  const counter = useRef(0)

  const cancel = useCallback(() => {
    stream.current?.cancel()
    stream.current = null
    active.current = 0
  }, [])

  useEffect(() => cancel, [cancel])

  const run = useCallback(
    (innovationId: string, institution: Institution) => {
      cancel()
      counter.current += 1
      const id = counter.current
      active.current = id
      const isCurrent = () => active.current === id

      setState({ ...IDLE, phase: 'running' })
      const call = hubApi.adaptations.request(
        innovationId,
        new InstitutionJs(institution.type, institution.staffCount, institution.budgetPln, institution.context),
        {
          onStep: (step) => {
            if (!isCurrent()) return
            setState((s) => ({ ...s, steps: [...s.steps, step] }))
          },
        },
      )
      stream.current = call

      void call.result.then((result) => {
        if (!isCurrent()) return
        active.current = 0
        stream.current = null
        if (result.value) {
          setState((s) => ({ ...s, phase: 'done', response: result.value ?? null }))
        } else {
          setState((s) => ({
            ...s,
            phase: 'failed',
            error: result.error ? describeApiError(result.error) : 'Nie udało się uzyskać planu.',
            unauthorized: result.error?.status === 401,
          }))
        }
      })
    },
    [cancel],
  )

  const stop = useCallback(() => {
    cancel()
    setState((s) => (s.phase === 'running' ? { ...s, phase: 'stopped' } : s))
  }, [cancel])

  const reset = useCallback(() => {
    cancel()
    setState(IDLE)
  }, [cancel])

  return { state, run, stop, reset }
}
