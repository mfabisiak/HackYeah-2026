import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Fieldset,
  Group,
  Paper,
  Radio,
  Stack,
  Text,
  TextInput,
  Textarea,
  Title,
} from '@mantine/core'
import {
  IconAlertTriangle,
  IconArrowLeft,
  IconArrowRight,
  IconInfoCircle,
  IconPencil,
  IconSend,
} from '@tabler/icons-react'
import { useQueryClient } from '@tanstack/react-query'
import { CreateIdeaJs } from 'hubmi-client'
import { useNavigate } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import { ensureFreshToken } from '../../auth/keycloak'
import { ErrorSummary, PageHeader } from '../../components'
import { ERROR_TEXT_COLOR, INPUT_STYLES } from '../../components/formStyles'
import { CanvasHint } from '../applications/CanvasHint'
import {
  IDEA_STAGE_OPTIONS,
  INNOVATION_STAGE_NAMES,
  MAX_IDEA_ESSENCE_LENGTH,
  TARGET_GROUP_NAMES,
  TARGET_GROUP_OPTIONS,
} from './constants'
import { clearIdeaDraft, loadIdeaDraft, saveIdeaDraft, type IdeaDraft } from './localData'
import { IDEA_FIELD_IDS, STEP_FIELDS, validateIdea, type IdeaErrors, type IdeaField } from './validation'

const EMPTY_IDEA: IdeaDraft = { title: '', essence: '', targetGroups: [], stage: '' }

const STEP_TITLES = ['Na czym polega pomysł', 'Dla kogo jest pomysł', 'Na jakim etapie jest pomysł', 'Sprawdź i zgłoś'] as const

const FIELD_LABELS: Record<IdeaField, string> = {
  title: 'Tytuł pomysłu',
  essence: 'Istota pomysłu',
  targetGroups: 'Dla kogo jest pomysł',
  stage: 'Etap pomysłu',
}

const isEmpty = (idea: IdeaDraft): boolean =>
  idea.title === '' && idea.essence === '' && idea.targetGroups.length === 0 && idea.stage === ''

function stepOfField(field: IdeaField): number {
  return STEP_FIELDS.findIndex((fields) => fields.includes(field))
}

export function IdeaWizardPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { login } = useAuth()
  const [restored] = useState(loadIdeaDraft)
  const [idea, setIdea] = useState<IdeaDraft>(restored ?? EMPTY_IDEA)
  const [showRestoredNotice, setShowRestoredNotice] = useState(restored !== null)
  const [step, setStep] = useState(0)
  const [attempts, setAttempts] = useState(0)
  const [serverErrors, setServerErrors] = useState<IdeaErrors>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<{ message: string; expired: boolean } | null>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const mounted = useRef(false)
  const errorsShown = useRef(false)

  useEffect(() => {
    if (isEmpty(idea)) clearIdeaDraft()
    else saveIdeaDraft(idea)
  }, [idea])

  const attempted = attempts > 0
  const errors = useMemo<IdeaErrors>(() => {
    if (!attempted) return {}
    const all = { ...serverErrors, ...validateIdea(idea) }
    const onStep = STEP_FIELDS[step]
    return Object.fromEntries(
      Object.entries(all).filter(([field]) => step === STEP_TITLES.length - 1 || onStep.includes(field as IdeaField)),
    )
  }, [attempted, idea, serverErrors, step])

  const summaryItems = (Object.entries(errors) as [IdeaField, string][]).map(([field, message]) => ({
    fieldId: IDEA_FIELD_IDS[field],
    message: `${FIELD_LABELS[field]}: ${message}`,
  }))

  const hasErrors = summaryItems.length > 0
  useEffect(() => {
    errorsShown.current = hasErrors
  }, [hasErrors])

  useEffect(() => {
    document.title = `Zgłoś pomysł, krok ${step + 1} z ${STEP_TITLES.length}: ${STEP_TITLES[step]} | HubMI`
    if (!mounted.current) {
      mounted.current = true
      return
    }
    // When the step changed because of errors, the error summary has just taken focus; leave it there.
    if (!errorsShown.current) headingRef.current?.focus()
  }, [step])

  const patch = (changes: Partial<IdeaDraft>) => {
    setIdea((current) => ({ ...current, ...changes }))
    setServerErrors({})
  }

  const goNext = () => {
    const found = validateIdea(idea)
    const blocking = STEP_FIELDS[step].filter((field) => found[field])
    if (blocking.length > 0) {
      setAttempts((n) => n + 1)
      return
    }
    setAttempts(0)
    setStep(step + 1)
  }

  const submit = async () => {
    const found = validateIdea(idea)
    const firstInvalid = (Object.keys(found) as IdeaField[]).sort((a, b) => stepOfField(a) - stepOfField(b))[0]
    if (firstInvalid) {
      setStep(stepOfField(firstInvalid))
      setAttempts((n) => n + 1)
      return
    }
    setSubmitting(true)
    setSubmitError(null)
    if (!(await ensureFreshToken())) {
      setSubmitting(false)
      setSubmitError({ message: 'Twoja sesja wygasła. Zaloguj się ponownie. Wpisany pomysł zostanie zachowany w tej przeglądarce.', expired: true })
      return
    }
    const result = await hubApi.ideas.create(
      new CreateIdeaJs(idea.title.trim(), idea.essence.trim(), idea.targetGroups, idea.stage),
    )
    setSubmitting(false)
    if (result.value) {
      clearIdeaDraft()
      void queryClient.invalidateQueries({ queryKey: ['ideas', 'mine'] })
      navigate(`/pomysly/${result.value.id}`, { state: { justCreated: true } })
      return
    }
    const error = result.error
    if (error?.status === 401) {
      setSubmitError({ message: 'Twoja sesja wygasła. Zaloguj się ponownie. Wpisany pomysł zostanie zachowany w tej przeglądarce.', expired: true })
    } else if (error?.status === 400 && error.details.length > 0) {
      const mapped = Object.fromEntries(
        error.details
          .filter((d) => d.field in IDEA_FIELD_IDS)
          .map((d) => [d.field, d.message]),
      ) as IdeaErrors
      setServerErrors(mapped)
      const first = (Object.keys(mapped) as IdeaField[]).sort((a, b) => stepOfField(a) - stepOfField(b))[0]
      if (first) setStep(stepOfField(first))
      setAttempts((n) => n + 1)
    } else {
      setSubmitError({ message: error?.message ?? 'Nie udało się zgłosić pomysłu. Spróbuj ponownie.', expired: false })
    }
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (step === STEP_TITLES.length - 1) void submit()
    else goNext()
  }

  const startOver = () => {
    clearIdeaDraft()
    setIdea(EMPTY_IDEA)
    setShowRestoredNotice(false)
    setStep(0)
    setAttempts(0)
  }

  const isLast = step === STEP_TITLES.length - 1

  return (
    <Stack gap="lg" maw={820}>
      <PageHeader
        title="Zgłoś pomysł na innowację"
        subtitle="Krótka fiszka: na czym polega pomysł, dla kogo jest i na jakim jest etapie. Zajmie to kilka minut."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Moje pomysły', href: '/pomysly' }, { title: 'Nowy pomysł' }]}
      />

      {showRestoredNotice && (
        <Alert color="blue" variant="light" role="note" icon={<IconInfoCircle size={22} aria-hidden="true" />} title="Przywrócono niedokończony pomysł">
          <Stack gap="xs" align="flex-start">
            <Text>
              Wczytaliśmy to, co zacząłeś(-aś) wpisywać wcześniej. Szkic jest tylko w tej przeglądarce i znika po
              wylogowaniu.
            </Text>
            <Button variant="default" size="sm" onClick={startOver}>
              Zacznij od nowa
            </Button>
          </Stack>
        </Alert>
      )}

      <form onSubmit={onSubmit} noValidate aria-labelledby="idea-step-heading">
        <Stack gap="lg">
          <Stack gap={4}>
            <Text fw={600} c="dimmed">
              Krok {step + 1} z {STEP_TITLES.length}
            </Text>
            <Title order={2} size="h2" id="idea-step-heading" ref={headingRef} tabIndex={-1} style={{ outline: 'none' }}>
              {STEP_TITLES[step]}
            </Title>
          </Stack>

          {summaryItems.length > 0 && (
            <ErrorSummary title={`Popraw dane (${summaryItems.length})`} errors={summaryItems} focusKey={attempts} />
          )}

          {step === 0 && (
            <Stack gap="lg">
              <TextInput
                id={IDEA_FIELD_IDS.title}
                label="Tytuł pomysłu"
                description="Krótka nazwa, po której łatwo rozpoznać pomysł."
                value={idea.title}
                onChange={(event) => patch({ title: event.currentTarget.value })}
                error={errors.title}
                required
                size="md"
                styles={INPUT_STYLES}
              />
              <div>
                <Textarea
                  id={IDEA_FIELD_IDS.essence}
                  label="Istota pomysłu"
                  description="Opisz własnymi słowami, na czym polega pomysł i jaki problem rozwiązuje."
                  value={idea.essence}
                  onChange={(event) => patch({ essence: event.currentTarget.value })}
                  error={errors.essence}
                  required
                  autosize
                  minRows={6}
                  maxRows={16}
                  size="md"
                  styles={INPUT_STYLES}
                />
                <Group justify="flex-end" mt={4}>
                  <Text size="sm" c={idea.essence.trim().length > MAX_IDEA_ESSENCE_LENGTH ? ERROR_TEXT_COLOR : 'dimmed'}>
                    {idea.essence.trim().length > MAX_IDEA_ESSENCE_LENGTH
                      ? `Przekroczono limit o ${idea.essence.trim().length - MAX_IDEA_ESSENCE_LENGTH} znaków`
                      : `${idea.essence.trim().length} / ${MAX_IDEA_ESSENCE_LENGTH} znaków`}
                  </Text>
                </Group>
              </div>
              <CanvasHint />
            </Stack>
          )}

          {step === 1 && (
            <Fieldset legend="Dla kogo jest pomysł? (zaznacz wszystkie pasujące)" styles={{ legend: { fontSize: '1.1rem', fontWeight: 700 } }}>
              <Stack gap="sm">
                {TARGET_GROUP_OPTIONS.map((option, index) => (
                  <Checkbox
                    key={option.value}
                    id={index === 0 ? IDEA_FIELD_IDS.targetGroups : undefined}
                    label={option.label}
                    size="md"
                    checked={idea.targetGroups.includes(option.value)}
                    onChange={(event) =>
                      patch({
                        targetGroups: event.currentTarget.checked
                          ? [...idea.targetGroups, option.value]
                          : idea.targetGroups.filter((g) => g !== option.value),
                      })
                    }
                  />
                ))}
                {errors.targetGroups && (
                  <Text c={ERROR_TEXT_COLOR} fw={600} role="alert">
                    {errors.targetGroups}
                  </Text>
                )}
              </Stack>
            </Fieldset>
          )}

          {step === 2 && (
            <Radio.Group
              label="Na jakim etapie jest pomysł?"
              value={idea.stage}
              onChange={(stage) => patch({ stage })}
              error={errors.stage}
              required
              size="md"
              styles={{ label: { fontSize: '1.1rem', fontWeight: 700 }, error: { fontSize: '0.95rem', fontWeight: 600 } }}
            >
              <Stack gap="sm" mt="xs">
                {IDEA_STAGE_OPTIONS.map((option, index) => (
                  <Radio
                    key={option.value}
                    id={index === 0 ? IDEA_FIELD_IDS.stage : undefined}
                    value={option.value}
                    label={option.label}
                    description={option.hint}
                    size="md"
                  />
                ))}
              </Stack>
            </Radio.Group>
          )}

          {step === 3 && (
            <Stack gap="md">
              <Text>Sprawdź dane. Po zgłoszeniu pomysł zobaczą pracownicy ROPS, a Ty będziesz widzieć jego status w „Moich pomysłach”.</Text>
              <Paper withBorder p="md" radius="md">
                <Stack component="dl" gap="md" m={0}>
                  <Summary label="Tytuł pomysłu" onEdit={() => setStep(0)} value={idea.title} />
                  <Summary label="Istota pomysłu" onEdit={() => setStep(0)} value={idea.essence} />
                  <Summary
                    label="Dla kogo"
                    onEdit={() => setStep(1)}
                    value={idea.targetGroups.map((g) => TARGET_GROUP_NAMES[g] ?? g).join(', ')}
                  />
                  <Summary label="Etap" onEdit={() => setStep(2)} value={INNOVATION_STAGE_NAMES[idea.stage] ?? ''} />
                </Stack>
              </Paper>
              {submitError && (
                <Alert color={submitError.expired ? 'yellow' : 'red'} icon={<IconAlertTriangle size={22} aria-hidden="true" />} role="alert" title="Pomysł nie został zgłoszony">
                  <Stack gap="xs" align="flex-start">
                    <Text>{submitError.message}</Text>
                    {submitError.expired && <Button onClick={login}>Zaloguj się ponownie</Button>}
                  </Stack>
                </Alert>
              )}
            </Stack>
          )}

          <Group justify="space-between" mt="md">
            <Button
              variant="default"
              leftSection={<IconArrowLeft size={18} aria-hidden="true" />}
              onClick={() => {
                setAttempts(0)
                setStep(step - 1)
              }}
              disabled={step === 0}
            >
              Wstecz
            </Button>
            {isLast ? (
              <Button type="submit" size="lg" loading={submitting} leftSection={<IconSend size={20} aria-hidden="true" />}>
                Zgłoś pomysł
              </Button>
            ) : (
              <Button type="submit" rightSection={<IconArrowRight size={18} aria-hidden="true" />}>
                Dalej
              </Button>
            )}
          </Group>
        </Stack>
      </form>
    </Stack>
  )
}

function Summary({ label, value, onEdit }: { label: string; value: string; onEdit: () => void }) {
  return (
    <div>
      <Text component="dt" fw={700}>
        {label}
      </Text>
      <Box component="dd" m={0}>
        <Text style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{value.trim() || <i>nie wypełniono</i>}</Text>
        <Button
          variant="subtle"
          size="compact-md"
          leftSection={<IconPencil size={16} aria-hidden="true" />}
          onClick={onEdit}
          aria-label={`Zmień: ${label}`}
        >
          Zmień
        </Button>
      </Box>
    </div>
  )
}
