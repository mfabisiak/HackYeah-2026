// In-memory mock of the idea, call and application endpoints (VITE_MOCK=true and tests). It keeps state between
// requests, so the whole path idea card -> application -> submission works without a backend.
import { http, HttpResponse } from 'msw'
import { fromContentJson } from '../features/applications/form'
import { validateForm, type FieldIssue } from '../features/applications/validation'

const DAY_MS = 86_400_000
const daysFromNow = (days: number): string => new Date(Date.now() + days * DAY_MS).toISOString()

interface MockIdea {
  id: string
  title: string
  essence: string
  targetGroups: string[]
  stage: string
  status: string
  adminComment: string | null
  createdAt: string
}

interface MockApplication {
  id: string
  callId: string
  applicantId: string
  ideaId: string | null
  status: 'DRAFT' | 'SUBMITTED'
  formVersion: number
  title: string | null
  applicant: unknown
  description: string | null
  innovativeness: string | null
  problemDiagnosis: string | null
  socialArea: string | null
  audienceDescription: string | null
  expectedChange: string | null
  futureVision: string | null
  plan: unknown
  requestedGrantAmountGrosze: number | null
  projectTeam: string | null
  declarations: string[]
  submittedAt: string | null
  createdAt: string
  updatedAt: string
}

const CURRENT_USER = 'user-12345'

const seedIdeas = (): MockIdea[] => [
  {
    id: 'idea-001',
    title: 'Sąsiedzka kawiarenka internetowa dla seniorów',
    essence:
      'Cotygodniowe spotkania w świetlicy wiejskiej, na których wolontariusze z liceum uczą seniorów obsługi telefonu, e-urzędu i bankowości.',
    targetGroups: ['SENIORS', 'YOUTH'],
    stage: 'IDEA',
    status: 'IN_REVIEW',
    adminComment: 'Ciekawy pomysł. Prosimy o doprecyzowanie, kto będzie prowadził zajęcia.',
    createdAt: daysFromNow(-12),
  },
  {
    id: 'idea-002',
    title: 'Wspólny transport do przychodni',
    essence: 'Kierowcy-wolontariusze dowożą osoby bez własnego auta do przychodni w gminach bez komunikacji publicznej.',
    targetGroups: ['SENIORS', 'RESIDENTS'],
    stage: 'PILOT',
    status: 'ACCEPTED',
    adminComment: null,
    createdAt: daysFromNow(-40),
  },
]

const seedCalls = () => [
  {
    id: 'call-open',
    title: 'Nabór na mikroinnowacje społeczne – edycja jesienna',
    description:
      'ROPS w Krakowie wspiera testowanie nowych rozwiązań społecznych w gminach Małopolski. Grant pokrywa koszty przygotowania i testów innowacji.',
    opensAt: daysFromNow(-30),
    closesAt: daysFromNow(14),
    status: 'OPEN',
    fields: [],
  },
  {
    id: 'call-upcoming',
    title: 'Nabór na mikroinnowacje społeczne – edycja wiosenna',
    description: 'Kolejny nabór dla pomysłów z obszaru wsparcia seniorów i przeciwdziałania wykluczeniu.',
    opensAt: daysFromNow(45),
    closesAt: daysFromNow(75),
    status: 'UPCOMING',
    fields: [],
  },
  {
    id: 'call-closed',
    title: 'Nabór na mikroinnowacje społeczne – edycja letnia',
    description: 'Nabór zakończony. Wyniki zostały ogłoszone na stronie ROPS.',
    opensAt: daysFromNow(-120),
    closesAt: daysFromNow(-60),
    status: 'CLOSED',
    fields: [],
  },
]

const DECLARATIONS = {
  RODO: [
    ['RODO_ROPS', 'Oświadczam, że zapoznałem/am się z klauzulą informacyjną Regionalnego Ośrodka Polityki Społecznej w Krakowie dotyczącą przetwarzania danych osobowych.'],
    ['RODO_MINISTRY', 'Oświadczam, że zapoznałem/am się z klauzulą informacyjną ministra właściwego ds. rozwoju regionalnego dotyczącą przetwarzania danych osobowych w ramach programu.'],
  ],
  INDIVIDUAL: [
    ['INDIV_TRUTH', 'Oświadczam, że wszelkie informacje podane we wniosku są zgodne ze stanem faktycznym i prawnym.'],
    ['INDIV_NO_DOUBLE_FINANCING', 'Oświadczam, że działania objęte wnioskiem nie są i nie będą współfinansowane z innych środków publicznych (zakaz podwójnego finansowania).'],
    ['INDIV_CAPACITY', 'Oświadczam, że posiadam pełną zdolność do czynności prawnych i korzystam z pełni praw publicznych.'],
    ['INDIV_RULES_ACCEPTANCE', 'Oświadczam, że zapoznałem/am się z Regulaminem naboru i realizacji mikroinnowacji i akceptuję jego postanowienia.'],
  ],
  ENTITY: [
    ['ENTITY_TRUTH', 'Oświadczam, że reprezentowany podmiot działa zgodnie z prawem, a dane zawarte we wniosku są zgodne ze stanem faktycznym i prawnym.'],
    ['ENTITY_NO_DOUBLE_FINANCING', 'Oświadczam, że wydatki ujęte w budżecie innowacji nie są i nie będą finansowane z innych źródeł publicznych.'],
    ['ENTITY_NOT_EXCLUDED', 'Oświadczam, że reprezentowany podmiot nie podlega wykluczeniu z możliwości otrzymania dofinansowania.'],
    ['ENTITY_RULES_ACCEPTANCE', 'Oświadczam, że podmiot akceptuje w całości Regulamin naboru i realizacji mikroinnowacji.'],
  ],
  NON_FORMAL_GROUP: [
    ['GROUP_TRUTH', 'Oświadczam w imieniu grupy nieformalnej, że informacje zawarte we wniosku są zgodne ze stanem faktycznym.'],
    ['GROUP_NO_DOUBLE_FINANCING', 'Oświadczamy, że planowane działania nie podlegają podwójnemu finansowaniu ze środków publicznych.'],
    ['GROUP_PARTNERSHIP', 'Oświadczam, że wszyscy członkowie grupy nieformalnej wyrazili zgodę na reprezentowanie ich w naborze.'],
    ['GROUP_RULES_ACCEPTANCE', 'Oświadczamy, że zapoznaliśmy się z Regulaminem naboru i akceptujemy jego warunki.'],
  ],
} as const

type ApplicantKey = keyof Omit<typeof DECLARATIONS, 'RODO'>

const declarationsFor = (applicantType: string | null) => {
  const specific =
    applicantType === null
      ? [...DECLARATIONS.INDIVIDUAL, ...DECLARATIONS.ENTITY, ...DECLARATIONS.NON_FORMAL_GROUP]
      : (DECLARATIONS[applicantType as ApplicantKey] ?? [])
  return [...specific, ...DECLARATIONS.RODO].map(([id, text]) => ({
    id,
    category: id.startsWith('RODO') ? 'RODO' : applicantType,
    text,
    required: true,
  }))
}

const FIRST_NEW_ID = 100

const state = { ideas: seedIdeas(), applications: [] as MockApplication[], nextId: FIRST_NEW_ID }

/** Restores the initial data; tests call it between cases. */
export function resetApplicationMocks(): void {
  state.ideas = seedIdeas()
  state.applications = []
  state.nextId = FIRST_NEW_ID
}

const newId = (prefix: string): string => `${prefix}-${String(state.nextId++).padStart(3, '0')}`

const validationFailed = (message: string, issues: FieldIssue[]) =>
  HttpResponse.json(
    {
      code: 'VALIDATION_FAILED',
      message,
      details: issues.map((issue) => ({ field: issue.field, code: { type: 'INVALID_FORMAT' }, message: issue.message })),
    },
    { status: 400 },
  )

const conflict = (message: string) => HttpResponse.json({ code: 'CONFLICT', message }, { status: 409 })
const notFound = (message: string) => HttpResponse.json({ code: 'NOT_FOUND', message }, { status: 404 })

function page<T>(items: T[], url: URL) {
  const pageIndex = Number(url.searchParams.get('page') ?? '0')
  const size = Number(url.searchParams.get('size') ?? '20')
  return { items: items.slice(pageIndex * size, (pageIndex + 1) * size), page: pageIndex, size, total: items.length }
}

/** Size limits the real server applies to every saved draft (ApplicationService.validateDraftPayload). */
function draftPayloadIssues(body: Partial<MockApplication>): FieldIssue[] {
  const plan = body.plan as Record<string, { costGrosze: number }[]> | null | undefined
  const items = plan ? [plan.preparation, plan.testingPhase1, plan.testingPhase2].flatMap((rows) => rows ?? []) : []
  return [
    ...((body.title?.length ?? 0) > 300 ? [{ field: 'title', message: 'Tytuł nie może przekraczać 300 znaków' }] : []),
    ...items.flatMap((item, index) =>
      item.costGrosze < 0 || item.costGrosze > 10_000_000
        ? [{ field: `plan.items[${index}].costGrosze`, message: 'Koszt zadania musi mieścić się w przedziale 0..100000 PLN' }]
        : [],
    ),
  ]
}

const findCall = (id: string) => seedCalls().find((call) => call.id === id)

function toApplicationForm(application: MockApplication) {
  return fromContentJson(
    JSON.stringify({
      ...application,
      requestedGrantAmountGrosze: application.requestedGrantAmountGrosze,
    }),
  )
}

export const applicationHandlers = [
  http.get('/api/ideas/mine', ({ request }) => {
    const mine = [...state.ideas].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return HttpResponse.json(page(mine, new URL(request.url)))
  }),

  http.get('/api/ideas/:id', ({ params }) => {
    const idea = state.ideas.find((i) => i.id === params.id)
    return idea ? HttpResponse.json(idea) : notFound('Nie znaleziono pomysłu')
  }),

  http.post('/api/ideas', async ({ request }) => {
    const body = (await request.json()) as { title: string; essence: string; targetGroups: string[]; stage: string }
    const issues: FieldIssue[] = [
      ...(body.title.trim() === '' ? [{ field: 'title', message: 'Tytuł pomysłu nie może być pusty' }] : []),
      ...(body.essence.trim() === '' ? [{ field: 'essence', message: 'Istota pomysłu nie może być pusta' }] : []),
      ...(body.targetGroups.length === 0
        ? [{ field: 'targetGroups', message: 'Należy wybrać przynajmniej jedną grupę docelową' }]
        : []),
    ]
    if (issues.length > 0) return validationFailed('Nieprawidłowe dane pomysłu', issues)
    const idea: MockIdea = {
      id: newId('idea'),
      title: body.title.trim(),
      essence: body.essence.trim(),
      targetGroups: body.targetGroups,
      stage: body.stage,
      status: 'SUBMITTED',
      adminComment: null,
      createdAt: new Date().toISOString(),
    }
    state.ideas = [...state.ideas, idea]
    return HttpResponse.json(idea, { status: 201 })
  }),

  http.get('/api/calls', ({ request }) => {
    const status = new URL(request.url).searchParams.get('status')
    return HttpResponse.json(seedCalls().filter((call) => status === null || call.status === status))
  }),

  http.get('/api/calls/active', () => HttpResponse.json(seedCalls().filter((call) => call.status === 'OPEN'))),

  http.get('/api/calls/:id/declarations', ({ params, request }) => {
    if (!findCall(String(params.id))) return notFound('Nie znaleziono naboru')
    const applicantType = new URL(request.url).searchParams.get('applicantType')
    return HttpResponse.json({ formVersion: 1, declarations: declarationsFor(applicantType) })
  }),

  http.get('/api/calls/:id', ({ params }) => {
    const call = findCall(String(params.id))
    return call ? HttpResponse.json(call) : notFound('Nie znaleziono naboru')
  }),

  http.post('/api/calls/:id/applications', async ({ params, request }) => {
    const call = findCall(String(params.id))
    if (!call) return notFound('Nie znaleziono naboru')
    if (call.status !== 'OPEN') return conflict('Nabór nie jest obecnie otwarty')
    if (state.applications.filter((a) => a.callId === call.id && a.status === 'DRAFT').length >= 5) {
      return conflict('Osiągnięto limit 5 szkiców wniosków w tym naborze')
    }
    const body = (await request.json().catch(() => null)) as { ideaId?: string | null } | null
    const idea = body?.ideaId ? state.ideas.find((i) => i.id === body.ideaId) : undefined
    if (body?.ideaId && !idea) return notFound('Nie znaleziono pomysłu')
    const now = new Date().toISOString()
    const application: MockApplication = {
      id: newId('app'),
      callId: call.id,
      applicantId: CURRENT_USER,
      ideaId: idea?.id ?? null,
      status: 'DRAFT',
      formVersion: 1,
      title: idea?.title ?? null,
      applicant: null,
      description: idea?.essence ?? null,
      innovativeness: null,
      problemDiagnosis: idea?.essence ?? null,
      socialArea: null,
      audienceDescription: null,
      expectedChange: null,
      futureVision: null,
      plan: null,
      requestedGrantAmountGrosze: null,
      projectTeam: null,
      declarations: [],
      submittedAt: null,
      createdAt: now,
      updatedAt: now,
    }
    state.applications = [...state.applications, application]
    return HttpResponse.json(application, { status: 201 })
  }),

  http.get('/api/applications/mine', ({ request }) => {
    const mine = [...state.applications].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    return HttpResponse.json(page(mine, new URL(request.url)))
  }),

  http.get('/api/applications/:id', ({ params }) => {
    const application = state.applications.find((a) => a.id === params.id)
    return application ? HttpResponse.json(application) : notFound('Nie znaleziono wniosku')
  }),

  http.put('/api/applications/:id', async ({ params, request }) => {
    const existing = state.applications.find((a) => a.id === params.id)
    if (!existing) return notFound('Nie znaleziono wniosku')
    if (existing.status !== 'DRAFT') {
      return conflict('Nie można edytować wniosku o statusie SUBMITTED (tylko szkice mogą być edytowane)')
    }
    const body = (await request.json()) as Partial<MockApplication>
    const draftIssues = draftPayloadIssues(body)
    if (draftIssues.length > 0) return validationFailed('Błędy rozmiaru danych szkicu', draftIssues)
    const updated: MockApplication = { ...existing, ...body, updatedAt: new Date().toISOString() }
    state.applications = state.applications.map((a) => (a.id === existing.id ? updated : a))
    return HttpResponse.json(updated)
  }),

  http.post('/api/applications/:id/submit', ({ params }) => {
    const existing = state.applications.find((a) => a.id === params.id)
    if (!existing) return notFound('Nie znaleziono wniosku')
    if (existing.status !== 'DRAFT') return conflict('Wniosek został już złożony i jest niezmienny')
    const call = findCall(existing.callId)
    if (call?.status !== 'OPEN') return conflict('Nabór jest zamknięty. Nie można złożyć wniosku.')
    const form = toApplicationForm(existing)
    const required = existing.applicant
      ? declarationsFor((existing.applicant as { type: string }).type).map((d) => d.id)
      : []
    const issues = validateForm(form, required)
    if (issues.length > 0) return validationFailed('Formularz wniosku zawiera błędy walidacji', issues)
    if (state.applications.filter((a) => a.callId === existing.callId && a.status === 'SUBMITTED').length >= 2) {
      return conflict('Osiągnięto maksymalny limit 2 złożonych wniosków w tym naborze')
    }
    const now = new Date().toISOString()
    const submitted: MockApplication = { ...existing, status: 'SUBMITTED', submittedAt: now, updatedAt: now }
    state.applications = state.applications.map((a) => (a.id === existing.id ? submitted : a))
    return HttpResponse.json(submitted)
  }),
]
