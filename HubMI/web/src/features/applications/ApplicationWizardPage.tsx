import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  Alert,
  Anchor,
  Button,
  Grid,
  Group,
  Modal,
  Paper,
  Stack,
  Text,
  Title,
  VisuallyHidden,
} from '@mantine/core'
import { IconAlertTriangle, IconArrowLeft, IconArrowRight, IconDeviceFloppy, IconSend } from '@tabler/icons-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import type { ApplicationJs } from 'hubmi-client'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { ApiClientError, formatApiError } from '../../api/errors'
import { unwrapApiResult } from '../../api/queryClient'
import { useAuth } from '../../auth/AuthContext'
import { ensureFreshToken } from '../../auth/keycloak'
import { ErrorAlert, ErrorSummary, LoadingState, PageHeader } from '../../components'
import { formatTime } from '../../components/dates'
import { ERROR_TEXT_COLOR } from '../../components/formStyles'
import { AnnouncerContext } from './announcer'
import { ApplicantStep } from './ApplicantStep'
import { ApplicationSummary } from './ApplicationSummary'
import { CanvasHint } from './CanvasHint'
import { DeclarationsFieldset, type DeclarationOption } from './DeclarationsFieldset'
import { IssuesProvider } from './fields'
import { fromContentJson, type ApplicantForm, type ApplicationForm } from './form'
import { PlanStep } from './PlanStep'
import { STEPS, fieldContext, fieldIdOrStep, stepForField, type StepId } from './steps'
import { SubmittedView } from './SubmittedView'
import { AudienceStep, BudgetStep, DescriptionStep, DiagnosisStep, TitleStep } from './TextSteps'
import { useDraftSaving } from './useDraftSaving'
import { draftIssues, validateForm, type FieldIssue } from './validation'

const STEP_PARAM = 'krok'
const SUBMIT_BUTTON_ID = 'submit-application'

function stepIndexFromParam(value: string | null): number {
  const n = Number(value)
  return Number.isInteger(n) && n >= 1 && n <= STEPS.length ? n - 1 : 0
}

/** The first message per field wins: client-side issues come first, then what the server added. */
function mergeIssues(...lists: readonly (readonly FieldIssue[])[]): FieldIssue[] {
  const seen = new Set<string>()
  return lists.flat().filter((issue) => !seen.has(issue.field) && seen.add(issue.field))
}

export function ApplicationWizardPage() {
  const { id = '' } = useParams()
  const [justSubmitted, setJustSubmitted] = useState(false)
  const application = useQuery({
    queryKey: ['application', id],
    queryFn: () => unwrapApiResult(hubApi.applications.get(id)),
    // The editor owns the form state once it is loaded; never serve a stale copy when coming back to the page.
    gcTime: 0,
    staleTime: 0,
    refetchOnMount: 'always',
    // A failed background refetch must never replace the editor and the unsaved edits in it.
    refetchOnReconnect: false,
  })
  const data = application.data
  const form = useMemo(() => (data ? fromContentJson(data.contentJson) : null), [data])

  const submitted = data?.status === 'SUBMITTED'
  useEffect(() => {
    document.title = submitted ? 'Wniosek złożony | HubMI' : 'Wniosek | HubMI'
  }, [submitted])

  if (application.isPending) return <LoadingState message="Wczytywanie wniosku..." />
  if (!data || !form) {
    const notFound = application.error instanceof ApiClientError && application.error.status === 404
    return (
      <Stack>
        <PageHeader
          title="Wniosek"
          breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje wnioski', href: '/wnioski' }, { title: 'Wniosek' }]}
        />
        <ErrorAlert
          title={notFound ? 'Nie znaleziono wniosku' : 'Nie udało się wczytać wniosku'}
          message={notFound ? 'Ten wniosek nie istnieje albo nie masz do niego dostępu.' : formatApiError(application.error)}
          onRetry={notFound ? undefined : () => void application.refetch()}
        />
        <Anchor component={Link} to="/wnioski">
          Wróć do moich wniosków
        </Anchor>
      </Stack>
    )
  }

  if (data.status === 'SUBMITTED') {
    return (
      <Stack>
        <PageHeader
          title={form.title || 'Wniosek'}
          subtitle="Wniosek o mikrogrant ROPS"
          breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje wnioski', href: '/wnioski' }, { title: 'Wniosek' }]}
        />
        <SubmittedView
          applicationId={data.id}
          submittedAt={data.submittedAt ?? null}
          form={form}
          justSubmitted={justSubmitted}
        />
      </Stack>
    )
  }

  return <ApplicationEditor key={data.id} application={data} initialForm={form} onSubmitted={() => setJustSubmitted(true)} />
}

interface ApplicationEditorProps {
  application: ApplicationJs
  initialForm: ApplicationForm
  onSubmitted: () => void
}

function ApplicationEditor({ application, initialForm, onSubmitted }: ApplicationEditorProps) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { login } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [form, setForm] = useState(initialForm)
  const [attempts, setAttempts] = useState(0)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitRejected, setSubmitRejected] = useState<{ body: string; issues: FieldIssue[] } | null>(null)
  const [announcement, setAnnouncement] = useState('')
  const headingRef = useRef<HTMLHeadingElement>(null)
  const pendingFocus = useRef<string | null>(null)
  const summaryHasErrors = useRef(false)
  const mounted = useRef(false)

  const stepIndex = stepIndexFromParam(searchParams.get(STEP_PARAM))
  const step = STEPS[stepIndex]

  const onSaved = useCallback(
    (saved: ApplicationJs) => {
      queryClient.setQueryData(['application', application.id], saved)
      void queryClient.invalidateQueries({ queryKey: ['applications', 'mine'] })
    },
    [queryClient, application.id],
  )
  const saving = useDraftSaving(application.id, form, onSaved)
  const { saveNow } = saving

  const call = useQuery({
    queryKey: ['call', application.callId],
    queryFn: () => unwrapApiResult(hubApi.calls.get(application.callId)),
  })
  const applicantType = form.applicant?.type ?? null
  const declarations = useQuery({
    queryKey: ['declarations', application.callId, applicantType],
    queryFn: () => unwrapApiResult(hubApi.calls.declarations(application.callId, applicantType)),
    enabled: applicantType !== null,
  })
  const declarationOptions = useMemo<DeclarationOption[] | null>(
    () =>
      applicantType === null
        ? null
        : (declarations.data?.declarations ?? []).map((d) => ({ id: d.id, text: d.text, required: d.required })),
    [applicantType, declarations.data],
  )
  const requiredDeclarations = useMemo(
    () => (declarationOptions ?? []).filter((d) => d.required).map((d) => d.id),
    [declarationOptions],
  )

  const clientIssues = useMemo(() => validateForm(form, requiredDeclarations), [form, requiredDeclarations])
  const attempted = attempts > 0
  const { body, serverIssues } = saving
  const issues = useMemo(
    () =>
      mergeIssues(
        attempted ? clientIssues : draftIssues(form),
        serverIssues,
        submitRejected?.body === body ? submitRejected.issues : [],
      ),
    [attempted, clientIssues, form, serverIssues, submitRejected, body],
  )
  const errorFor = useMemo(() => {
    const byField = new Map(issues.map((i) => [i.field, i.message]))
    return (path: string) => byField.get(path)
  }, [issues])

  const summaryItems = useMemo(() => {
    const items = issues.map((issue) => ({
      fieldId: fieldIdOrStep(issue.field),
      message: `${fieldContext(issue.field)}: ${issue.message}`,
      path: issue.field,
    }))
    return items.filter((item, index) => items.findIndex((other) => other.fieldId === item.fieldId) === index)
  }, [issues])
  const issuesPerStep = useMemo(
    () =>
      STEPS.map((s) => issues.filter((issue) => (stepForField(issue.field) ?? step.id) === s.id).length),
    [issues, step.id],
  )
  const summaryVisible = step.id === 'summary' && attempted && summaryItems.length > 0

  useEffect(() => {
    summaryHasErrors.current = summaryVisible
  }, [summaryVisible])

  // After the user moves to another step, focus lands on the requested field or on the step heading.
  useEffect(() => {
    document.title = `Wniosek, krok ${stepIndex + 1} z ${STEPS.length}: ${step.title} | HubMI`
    if (!mounted.current) {
      mounted.current = true
      return
    }
    const requested = pendingFocus.current
    pendingFocus.current = null
    const element = requested ? document.getElementById(requested) : null
    if (!element && step.id === 'summary' && summaryHasErrors.current) return
    const target = element ?? headingRef.current
    target?.focus()
    element?.scrollIntoView?.({ block: 'center' })
  }, [stepIndex, step.id, step.title])

  const announce = useCallback((message: string) => {
    setAnnouncement((previous) => (previous === message ? `${message}\u00a0` : message))
  }, [])

  const goToStep = useCallback(
    (index: number, focusFieldId?: string) => {
      pendingFocus.current = focusFieldId ?? null
      setSearchParams(
        (previous) => {
          const next = new URLSearchParams(previous)
          next.set(STEP_PARAM, String(index + 1))
          return next
        },
        { replace: false },
      )
      void saveNow()
    },
    [setSearchParams, saveNow],
  )

  const goToField = (fieldId: string) => {
    const item = summaryItems.find((i) => i.fieldId === fieldId)
    const target = item ? STEPS.findIndex((s) => s.id === stepForField(item.path)) : -1
    if (target < 0 || target === stepIndex) {
      document.getElementById(fieldId)?.focus()
    } else {
      goToStep(target, fieldId)
    }
  }

  const patch = (changes: Partial<ApplicationForm>) => setForm((current) => ({ ...current, ...changes }))
  const setApplicant = (applicant: ApplicantForm) =>
    setForm((current) => ({
      ...current,
      applicant,
      declarations: current.applicant?.type === applicant.type ? current.declarations : [],
    }))

  const callClosed = call.data !== undefined && call.data.status !== 'OPEN'

  const requestSubmit = () => {
    setSubmitError(null)
    if (applicantType !== null && declarations.isPending) {
      setSubmitError('Oświadczenia jeszcze się wczytują. Spróbuj za chwilę.')
      return
    }
    setAttempts((n) => n + 1)
    if (validateForm(form, requiredDeclarations).length > 0 || declarations.isError) return
    setConfirmOpen(true)
  }

  // Focus goes back to the button that opened the dialog only when the user backs out; after an error the error
  // summary keeps it.
  const closeConfirm = () => {
    if (submitting) return
    setConfirmOpen(false)
    window.setTimeout(() => document.getElementById(SUBMIT_BUTTON_ID)?.focus(), 0)
  }

  const confirmSubmit = async () => {
    setSubmitting(true)
    setSubmitError(null)
    const stored = await saving.saveNow()
    if (!stored) {
      setSubmitting(false)
      setConfirmOpen(false)
      setSubmitError('Najpierw trzeba zapisać szkic, a zapis się nie udał. Wniosek nie został złożony.')
      return
    }
    if (!(await ensureFreshToken())) {
      setSubmitting(false)
      setConfirmOpen(false)
      setSubmitError(
        navigator.onLine
          ? 'Twoja sesja wygasła. Zaloguj się ponownie – szkic jest zapisany, wniosek nie został złożony.'
          : 'Brak połączenia z internetem. Wniosek nie został złożony – spróbuj ponownie, gdy połączenie wróci.',
      )
      return
    }
    const result = await hubApi.applications.submit(application.id)
    setSubmitting(false)
    setConfirmOpen(false)
    if (result.value) {
      queryClient.setQueryData(['application', application.id], result.value)
      void queryClient.invalidateQueries({ queryKey: ['applications', 'mine'] })
      onSubmitted()
      return
    }
    const error = result.error
    if (error?.status === 400 && error.details.length > 0) {
      setSubmitRejected({
        body: saving.body,
        issues: error.details.map((d) => ({ field: d.field, message: d.message })),
      })
      setAttempts((n) => n + 1)
    } else if (error?.status === 401) {
      setSubmitError('Twoja sesja wygasła. Zaloguj się ponownie – szkic jest zapisany, wniosek nie został złożony.')
    } else {
      setSubmitError(error?.message ?? 'Nie udało się złożyć wniosku. Spróbuj ponownie.')
    }
  }

  const unsavable = draftIssues(form).length > 0
  const savedText = saving.savedAt ? `Zapisano o ${formatTime(saving.savedAt)}` : null
  const statusText = saving.saving
    ? 'Zapisywanie szkicu…'
    : saving.dirty
      ? 'Zmiany czekają na zapis'
      : (savedText ?? 'Szkic zapisuje się automatycznie na serwerze')

  const isFirst = stepIndex === 0
  const isLast = stepIndex === STEPS.length - 1
  const stepErrorCount = issuesPerStep[stepIndex]

  const renderStep = () => {
    switch (step.id) {
      case 'title':
        return (
          <Stack gap="lg">
            <TitleStep form={form} onChange={patch} />
            <CanvasHint />
          </Stack>
        )
      case 'applicant':
        return <ApplicantStep applicant={form.applicant} onChange={setApplicant} />
      case 'description':
        return (
          <Stack gap="lg">
            <DescriptionStep form={form} onChange={patch} />
            <CanvasHint />
          </Stack>
        )
      case 'diagnosis':
        return <DiagnosisStep form={form} onChange={patch} />
      case 'audience':
        return <AudienceStep form={form} onChange={patch} />
      case 'plan':
        return <PlanStep plan={form.plan} onChange={(plan) => patch({ plan })} />
      case 'budget':
        return <BudgetStep form={form} onChange={patch} />
      case 'summary':
        return (
          <Stack gap="xl">
            {summaryVisible && (
              <ErrorSummary
                title={`Wniosek wymaga poprawek (${summaryItems.length})`}
                errors={summaryItems}
                onNavigate={goToField}
                focusKey={attempts}
              />
            )}
            <Stack gap="md">
              <Title order={3} size="h4">
                Sprawdź wniosek
              </Title>
              <ApplicationSummary form={form} onEdit={(id: StepId) => goToStep(STEPS.findIndex((s) => s.id === id))} />
            </Stack>
            {declarations.isPending && applicantType !== null ? (
              <LoadingState message="Wczytywanie oświadczeń..." minHeight={80} />
            ) : declarations.isError ? (
              <ErrorAlert
                title="Nie udało się wczytać oświadczeń"
                message={formatApiError(declarations.error)}
                onRetry={() => void declarations.refetch()}
              />
            ) : (
              <DeclarationsFieldset
                declarations={declarationOptions}
                accepted={form.declarations}
                onChange={(accepted) => patch({ declarations: accepted })}
                onChooseApplicant={() => goToStep(1)}
              />
            )}
            {submitError && (
              <Alert color="red" icon={<IconAlertTriangle size={22} aria-hidden="true" />} role="alert" title="Nie złożono wniosku">
                {submitError}
              </Alert>
            )}
            {callClosed ? (
              <Alert color="yellow" role="note" icon={<IconAlertTriangle size={22} aria-hidden="true" />} title="Nabór jest zamknięty">
                Wniosku nie można już złożyć. Szkic pozostaje zapisany.
              </Alert>
            ) : (
              <Group>
                <Button id={SUBMIT_BUTTON_ID} size="lg" leftSection={<IconSend size={20} aria-hidden="true" />} onClick={requestSubmit}>
                  Złóż wniosek
                </Button>
              </Group>
            )}
          </Stack>
        )
    }
  }

  const onFormSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (isLast) requestSubmit()
    else goToStep(stepIndex + 1)
  }

  return (
    <AnnouncerContext value={announce}>
      <IssuesProvider errorFor={errorFor}>
        <PageHeader
          title="Wniosek o mikrogrant"
          subtitle={call.data ? `Nabór: ${call.data.title}` : undefined}
          breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje wnioski', href: '/wnioski' }, { title: 'Wniosek' }]}
        />
        <VisuallyHidden role="status">{announcement}</VisuallyHidden>

        <Paper withBorder p="sm" radius="md" mb="lg">
          <Group justify="space-between" gap="sm">
            <div>
              <Text fw={600}>{statusText}</Text>
              {unsavable && (
                <Text size="sm" c={ERROR_TEXT_COLOR} fw={600}>
                  Część pól ma błędy i nie zostanie zapisana, dopóki ich nie poprawisz.
                </Text>
              )}
            </div>
            {/* Only the result is announced; “changes waiting” and “saving” would repeat on every pause in typing. */}
            <VisuallyHidden role="status">{!saving.dirty && !saving.saving ? savedText : ''}</VisuallyHidden>
            <Group gap="sm">
              <Button
                variant="light"
                leftSection={<IconDeviceFloppy size={18} aria-hidden="true" />}
                onClick={() => void saving.saveNow()}
              >
                Zapisz szkic
              </Button>
              <Button
                variant="default"
                onClick={() => void saving.saveNow().then((ok) => ok && navigate('/wnioski'))}
              >
                Zapisz i wróć do listy
              </Button>
            </Group>
          </Group>
        </Paper>

        {saving.failure && (
          <Alert
            color={saving.failure.reason === 'expired' ? 'yellow' : 'red'}
            icon={<IconAlertTriangle size={22} aria-hidden="true" />}
            title="Szkic nie został zapisany"
            role="alert"
            mb="lg"
          >
            <Stack gap="xs" align="flex-start">
              <Text>{saving.failure.message}</Text>
              {saving.failure.reason === 'expired' ? (
                <>
                  <Text size="sm">
                    Zmiany wpisane po ostatnim zapisie{saving.savedAt ? ` (${formatTime(saving.savedAt)})` : ''} mogą
                    zniknąć po przejściu do logowania.
                  </Text>
                  <Button onClick={login}>Zaloguj się ponownie</Button>
                </>
              ) : (
                <Button variant="outline" color="red" onClick={() => void saving.saveNow()}>
                  Spróbuj zapisać ponownie
                </Button>
              )}
            </Stack>
          </Alert>
        )}

        <Grid gap="xl">
          <Grid.Col span={{ base: 12, md: 4, lg: 3 }}>
            <nav aria-label="Kroki wniosku">
              <Stack component="ol" gap={4} p={0} m={0} style={{ listStyle: 'none' }}>
                {STEPS.map((s, index) => (
                  <li key={s.id}>
                    <Button
                      fullWidth
                      justify="flex-start"
                      variant={index === stepIndex ? 'light' : 'subtle'}
                      color={issuesPerStep[index] > 0 ? 'red' : undefined}
                      aria-current={index === stepIndex ? 'step' : undefined}
                      onClick={() => goToStep(index)}
                      styles={{ label: { whiteSpace: 'normal', textAlign: 'left' } }}
                    >
                      {index + 1}. {s.title}
                      {issuesPerStep[index] > 0 ? ` (do poprawy: ${issuesPerStep[index]})` : ''}
                    </Button>
                  </li>
                ))}
              </Stack>
            </nav>
          </Grid.Col>

          <Grid.Col span={{ base: 12, md: 8, lg: 9 }}>
            <form onSubmit={onFormSubmit} noValidate aria-labelledby="step-heading">
              <Stack gap="lg">
                <Stack gap={4}>
                  <Text fw={600} c="dimmed">
                    Krok {stepIndex + 1} z {STEPS.length} · {step.points} wzoru ROPS
                  </Text>
                  <Title order={2} size="h2" id="step-heading" ref={headingRef} tabIndex={-1} style={{ outline: 'none' }}>
                    {step.title}
                  </Title>
                  {stepErrorCount > 0 && step.id !== 'summary' && (
                    <Text c={ERROR_TEXT_COLOR} fw={600} role="status">
                      <IconAlertTriangle size={18} aria-hidden="true" style={{ verticalAlign: 'text-bottom' }} /> Do
                      poprawienia w tym kroku: {stepErrorCount}
                    </Text>
                  )}
                </Stack>

                {renderStep()}

                <Group justify="space-between" mt="md">
                  <Button
                    variant="default"
                    leftSection={<IconArrowLeft size={18} aria-hidden="true" />}
                    onClick={() => goToStep(stepIndex - 1)}
                    disabled={isFirst}
                  >
                    Wstecz
                  </Button>
                  {!isLast && (
                    <Button type="submit" rightSection={<IconArrowRight size={18} aria-hidden="true" />}>
                      Dalej: {STEPS[stepIndex + 1].title}
                    </Button>
                  )}
                </Group>
              </Stack>
            </form>
          </Grid.Col>
        </Grid>

        <Modal
          opened={confirmOpen}
          onClose={closeConfirm}
          title="Złożyć wniosek?"
          size="lg"
          trapFocus
          returnFocus={false}
          closeOnEscape
        >
          <Stack gap="md">
            <Text>
              Po złożeniu wniosku nie będzie można go już zmienić. Sprawdź, czy wszystkie dane są poprawne. W jednym
              naborze można złożyć najwyżej 2 wnioski.
            </Text>
            <Group justify="flex-end">
              <Button variant="default" onClick={closeConfirm} disabled={submitting} data-autofocus>
                Wróć do wniosku
              </Button>
              <Button onClick={() => void confirmSubmit()} loading={submitting}>
                Tak, złóż wniosek
              </Button>
            </Group>
          </Stack>
        </Modal>
      </IssuesProvider>
    </AnnouncerContext>
  )
}
