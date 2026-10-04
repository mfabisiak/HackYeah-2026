import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ApplicationJs } from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { ensureFreshToken } from '../../auth/keycloak'
import { PLAN_PHASES, toRequest, type ApplicationForm } from './form'
import type { FieldIssue } from './validation'

/** How long to wait after the last edit before the draft is saved on the server. */
const AUTOSAVE_DELAY_MS = 2500

export type SaveFailureReason = 'expired' | 'network' | 'conflict' | 'invalid'

/** The server numbers plan items across all phases (`plan.items[3]`); point them at the row on screen. */
function fieldOnScreen(field: string, form: ApplicationForm): string {
  const match = /^plan\.items\[(\d+)]\.(.+)$/.exec(field)
  if (!match) return field
  const flat = Number(match[1])
  const rows = PLAN_PHASES.flatMap((phase) => form.plan[phase].map((_, index) => ({ phase, index })))
  const row = rows[flat]
  return row ? `plan.${row.phase}[${row.index}].${match[2]}` : field
}

export interface SaveFailure {
  reason: SaveFailureReason
  message: string
}

export interface DraftSaving {
  saving: boolean
  savedAt: Date | null
  failure: SaveFailure | null
  /** Edits not yet stored on the server. */
  dirty: boolean
  /** Saves now (waiting for a save in progress); `true` when the server holds the latest edits. */
  saveNow: () => Promise<boolean>
  /** Problems the server reported for the last saved content; they disappear once the user edits again. */
  serverIssues: FieldIssue[]
  /** The JSON of the current form, which changes with every edit. */
  body: string
}

/**
 * Keeps the draft on the server (never in localStorage: it holds personal data). Saves are queued one at a time,
 * because the server rejects overlapping updates of the same draft.
 */
export function useDraftSaving(
  applicationId: string,
  form: ApplicationForm,
  onSaved: (application: ApplicationJs) => void,
): DraftSaving {
  // Non-breaking spaces (common in pasted NIPs and phone numbers) are not accepted by the server's formats.
  const body = useMemo(() => JSON.stringify(toRequest(form)).replace(/\u00a0/g, ' '), [form])
  const formRef = useRef(form)
  const latestBody = useRef(body)
  const savedBodyRef = useRef(body)
  const inFlight = useRef<Promise<boolean> | null>(null)
  const [savedBody, setSavedBody] = useState(body)
  const [saving, setSaving] = useState(false)
  const [savedAt, setSavedAt] = useState<Date | null>(null)
  const [failure, setFailure] = useState<SaveFailure | null>(null)
  const [rejected, setRejected] = useState<{ body: string; issues: FieldIssue[] } | null>(null)

  useEffect(() => {
    latestBody.current = body
    formRef.current = form
  }, [body, form])

  const saveNow = useCallback(async (): Promise<boolean> => {
    while (inFlight.current) await inFlight.current
    const current = latestBody.current
    if (current === savedBodyRef.current) return true

    const run = async (): Promise<boolean> => {
      setSaving(true)
      setFailure(null)
      if (!(await ensureFreshToken())) {
        setFailure(
          navigator.onLine
            ? { reason: 'expired', message: 'Twoja sesja wygasła. Zaloguj się ponownie, aby zapisać dalsze zmiany.' }
            : { reason: 'network', message: 'Brak połączenia z internetem. Zmiany zapiszą się, gdy połączenie wróci.' },
        )
        return false
      }
      const result = await hubApi.applications.saveDraft(applicationId, current)
      if (result.error || !result.value) {
        const error = result.error
        if (error?.status === 401) {
          setFailure({
            reason: 'expired',
            message: 'Twoja sesja wygasła. Zaloguj się ponownie, aby zapisać dalsze zmiany.',
          })
        } else if (error?.status === 400) {
          setRejected({
            body: current,
            issues: error.details.map((d) => ({ field: fieldOnScreen(d.field, formRef.current), message: d.message })),
          })
          setFailure({
            reason: 'invalid',
            message:
              error.details.length > 0
                ? 'Szkicu nie udało się zapisać. Popraw zaznaczone pola – do tego czasu nic nowego się nie zapisze.'
                : 'Szkicu nie da się zapisać w tej postaci. Sprawdź kwoty i długość tekstów.',
          })
        } else if (error?.status === 409) {
          setFailure({ reason: 'conflict', message: error.message })
        } else {
          setFailure({
            reason: 'network',
            message: 'Nie udało się zapisać szkicu. Sprawdź połączenie z internetem i spróbuj ponownie.',
          })
        }
        return false
      }
      savedBodyRef.current = current
      setSavedBody(current)
      setSavedAt(new Date())
      onSaved(result.value)
      return true
    }

    inFlight.current = run()
    try {
      return await inFlight.current
    } finally {
      inFlight.current = null
      setSaving(false)
    }
  }, [applicationId, onSaved])

  const dirty = body !== savedBody
  const needsLogin = failure?.reason === 'expired'

  useEffect(() => {
    if (!dirty || needsLogin) return
    const timer = window.setTimeout(() => void saveNow(), AUTOSAVE_DELAY_MS)
    return () => window.clearTimeout(timer)
  }, [body, dirty, needsLogin, saveNow])

  // Leaving the page (menu, breadcrumb, browser Back) must not lose what was typed in the last seconds.
  useEffect(() => () => void saveNow(), [saveNow])

  useEffect(() => {
    if (!dirty) return
    const warn = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  return {
    saving,
    savedAt,
    failure,
    dirty,
    saveNow,
    serverIssues: rejected?.body === body ? rejected.issues : [],
    body,
  }
}
