import { http, HttpResponse } from 'msw'
import { applicationHandlers } from './applications'

const mockInnovations = [
  {
    id: 'inno-001',
    title: 'Inteligentny asystent komunikacji dla seniorów',
    summary: 'Aplikacja mobilna upraszczająca kontakt z rodziną i służbami medycznymi.',
    description: 'Szczegółowy opis rozwiązania wspomagającego codzienne funkcjonowanie osób starszych.',
    areas: ['DIGITAL_EXCLUSION', 'AGING'],
    targetGroups: ['SENIORS', 'RESIDENTS'],
    stage: 'IMPLEMENTED',
    region: 'Kraków - Podgórze',
    mediaUrls: ['https://rops.krakow.pl/innowacje/asystent-seniora.pdf'],
    averageRating: 4.8,
    ratingsCount: 12,
    innovativeness: 'Intuicyjny interfejs o bardzo dużych przyciskach i kontrastowych barwach.',
    problemDiagnosis: 'Samotność seniorów i lęk przed skomplikowanymi smartfonami.',
    audienceDescription: 'Osoby w wieku 65+ mieszkające samotnie.',
    expectedChange: 'Zwiększenie samodzielności i poczucia bezpieczeństwa.',
    futureVision: 'Możliwość integracji z systemami teleopieki w całej Małopolsce.',
    createdAt: '2026-03-01T10:00:00Z',
    updatedAt: '2026-03-15T14:30:00Z',
  },
  {
    id: 'inno-002',
    title: 'Mobilny Punkt Wsparcia Sąsiedzkiego',
    summary: 'Sieć wzajemnej pomocy łącząca seniorów z zaufanymi sąsiadami z tej samej okolicy.',
    description: 'System organizacyjny wspierający lokalne społeczności wiejskie i miejskie.',
    areas: ['AGING', 'LONELINESS', 'SERVICE_ACCESS'],
    targetGroups: ['SENIORS', 'FAMILIES'],
    stage: 'TESTED',
    region: 'Tarnów',
    mediaUrls: ['https://youtube.com/watch?v=pomoc-sasiedzka'],
    averageRating: 4.5,
    ratingsCount: 8,
    innovativeness: 'Mikro-wolontariat oparty na relacjach sąsiedzkich.',
    problemDiagnosis: 'Brak codziennej pomocy w zakupach i drobnych naprawach.',
    audienceDescription: 'Seniorzy i osoby z trudnościami w poruszaniu się.',
    expectedChange: 'Zmniejszenie izolacji społecznej.',
    futureVision: 'Skalowanie do wszystkich sołectw regionu.',
    createdAt: '2026-03-10T12:00:00Z',
    updatedAt: '2026-03-20T09:15:00Z',
  },
]

const mockChallenges = [
  {
    id: 'chal-001',
    title: 'Transport na żądanie dla seniorów z sołectw podmiejskich',
    description: 'Osoby starsze mieszkające w mniejszych sołectwach mają utrudniony dostęp do placówek ochrony zdrowia i urzędów.',
    area: 'AGING',
    municipalities: ['Wieliczka', 'Niepołomice', 'Biskupice'],
    createdAt: '2026-02-15T08:00:00Z',
  },
  {
    id: 'chal-002',
    title: 'Osamotnienie seniorów w osiedlach wielkopłytowych',
    description: 'Bariery architektoniczne powodują uwięzienie w domach wielu samotnych seniorów.',
    area: 'LONELINESS',
    municipalities: ['Tarnów', 'Kraków'],
    createdAt: '2026-03-01T09:00:00Z',
  },
]

const mockMaterials = [
  {
    id: 'mat-001',
    title: 'Jak inkubować innowację społeczną – poradnik krok po kroku',
    description: 'Kompleksowy przewodnik dla animatorów i samorządowców opisujący etapy od diagnozy problemu po wdrożenie.',
    type: 'GUIDE',
    areas: ['COORDINATION', 'OTHER'],
    url: 'https://rops.krakow.pl/innowacje/poradnik-inkubacji.pdf',
    createdAt: '2026-01-10T10:00:00Z',
  },
  {
    id: 'mat-002',
    title: 'Model Canvas dla Innowacji Społecznych – szablon pracy',
    description: 'Narzędzie warsztatowe do projektowania propozycji wartości, odbiorców i wskaźników wpływu społecznego.',
    type: 'CANVAS',
    areas: ['OTHER'],
    url: 'https://rops.krakow.pl/materialy/canvas-innowacji.pdf',
    createdAt: '2026-01-15T10:00:00Z',
  },
  {
    id: 'mat-003',
    title: 'Dostępność cyfrowa stron i e-usług wg WCAG 2.1 AA',
    description: 'Instruktaż wideo dla twórców stron: semantyka HTML, nawigacja klawiaturą i kontrast.',
    type: 'VIDEO',
    areas: ['DIGITAL_EXCLUSION'],
    url: 'https://youtube.com/watch?v=dostepnosc-wcag',
    createdAt: '2026-02-01T10:00:00Z',
  },
]

export const handlers = [
  ...applicationHandlers,

  // Health
  http.get('/health', () => {
    return HttpResponse.json({ status: 'ok' })
  }),
  http.get('/health/ready', () => {
    return HttpResponse.json({ status: 'ready', database: 'connected' })
  }),

  // Me
  http.get('/api/me', () => {
    return HttpResponse.json({
      id: 'user-12345',
      username: 'jan.kowalski',
      email: 'jan.kowalski@example.com',
      roles: ['user', 'admin'],
    })
  }),

  // Admin
  http.get('/api/admin', () => {
    return HttpResponse.json({
      message: 'Panel administracyjny HubMI aktywny',
    })
  }),

  // Innovations
  http.get('/api/innovations', ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') ?? '0', 10)
    const size = parseInt(url.searchParams.get('size') ?? '10', 10)
    const q = url.searchParams.get('q')?.toLowerCase()

    let filtered = mockInnovations
    if (q) {
      filtered = filtered.filter(
        (i) => i.title.toLowerCase().includes(q) || i.summary.toLowerCase().includes(q),
      )
    }

    const items = filtered.slice(page * size, (page + 1) * size)
    return HttpResponse.json({
      items,
      page,
      size,
      total: filtered.length,
    })
  }),

  http.get('/api/innovations/:id', ({ params }) => {
    const item = mockInnovations.find((i) => i.id === params.id)
    if (!item) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: 'Innowacja nie została znaleziona' },
        { status: 404 },
      )
    }
    return HttpResponse.json(item)
  }),

  http.post('/api/innovations', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newInno = {
      id: `inno-${Date.now()}`,
      ...body,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json(newInno, { status: 201 })
  }),

  // Test requests
  http.get('/api/innovations/:id/test-request', ({ params }) => {
    return HttpResponse.json({
      id: `tr-${params.id}`,
      innovationId: params.id,
      note: 'Przykładowa notatka zgłoszenia testowego',
      status: 'NEW',
      createdAt: '2026-03-25T11:00:00Z',
    })
  }),

  http.put('/api/innovations/:id/test-request', async ({ params, request }) => {
    const body = (await request.json()) as { note?: string }
    return HttpResponse.json({
      id: `tr-${params.id}`,
      innovationId: params.id,
      note: body.note ?? null,
      status: 'NEW',
      createdAt: new Date().toISOString(),
    })
  }),

  // Feedback
  http.get('/api/innovations/:id/feedback', ({ params }) => {
    return HttpResponse.json({
      id: `fb-${params.id}`,
      innovationId: params.id,
      rating: 5,
      comment: 'Świetne i potrzebne rozwiązanie!',
      suggestion: 'Więcej warsztatów stacjonarnych',
      createdAt: '2026-03-25T11:30:00Z',
    })
  }),

  http.put('/api/innovations/:id/feedback', async ({ params, request }) => {
    const body = (await request.json()) as { rating: number; comment?: string; suggestion?: string }
    return HttpResponse.json({
      id: `fb-${params.id}`,
      innovationId: params.id,
      rating: body.rating,
      comment: body.comment ?? null,
      suggestion: body.suggestion ?? null,
      createdAt: new Date().toISOString(),
    })
  }),

  // Challenges
  http.get('/api/challenges', ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') ?? '0', 10)
    const size = parseInt(url.searchParams.get('size') ?? '10', 10)

    const items = mockChallenges.slice(page * size, (page + 1) * size)
    return HttpResponse.json({
      items,
      page,
      size,
      total: mockChallenges.length,
    })
  }),

  http.get('/api/challenges/:id', ({ params }) => {
    const item = mockChallenges.find((c) => c.id === params.id)
    if (!item) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: 'Wyzwanie nie zostało znalezione' },
        { status: 404 },
      )
    }
    return HttpResponse.json(item)
  }),

  // Materials
  http.get('/api/materials', ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') ?? '0', 10)
    const size = parseInt(url.searchParams.get('size') ?? '10', 10)
    const type = url.searchParams.get('type')
    const filtered = mockMaterials.filter((m) => type === null || m.type === type)

    return HttpResponse.json({
      items: filtered.slice(page * size, (page + 1) * size),
      page,
      size,
      total: filtered.length,
    })
  }),

  // Matching
  http.post('/api/matches', async ({ request }) => {
    const body = (await request.json()) as { description?: string; municipality?: string }
    const desc = body.description?.toLowerCase() ?? ''

    if (desc.includes('brak') || desc.includes('kosmos')) {
      return HttpResponse.json({
        needId: 'need-mock-empty',
        matches: [],
        similarNeeds: [],
        noGoodMatch: true,
      })
    }

    return HttpResponse.json({
      needId: 'need-mock-1',
      matches: [
        {
          innovation: {
            id: 'inno-001',
            title: 'Inteligentny asystent komunikacji dla seniorów',
            summary: 'Aplikacja mobilna upraszczająca kontakt z rodziną i służbami medycznymi.',
            areas: ['DIGITAL_EXCLUSION', 'AGING'],
            targetGroups: ['SENIORS'],
            stage: 'TESTED',
          },
          score: 0.88,
          reasons: [
            'Innowacja bezpośrednio rozwiązuje problem komunikacji i kontaktu ze służbami zdrowotnymi.',
            'Rozwiązanie zostało z sukcesem przetestowane w warunkach domowych z seniorami w Małopolsce.',
          ],
          matchedTerms: ['senior', 'komunikacja', 'lekarz'],
        },
        {
          innovation: {
            id: 'inno-002',
            title: 'Mobilny punkt wsparcia i animacji społecznej dla gmin wiejskich',
            summary: 'Specjalny bus wyposażony w sprzęt edukacyjny i przestrzeń integracyjną docierający do małych miejscowości.',
            areas: ['SERVICE_ACCESS', 'LONELINESS'],
            targetGroups: ['RESIDENTS', 'SENIORS'],
            stage: 'IMPLEMENTED',
          },
          score: 0.64,
          reasons: [
            'Model mobilnych usług przeciwdziała wykluczeniu komunikacyjnemu w małych gminach.',
          ],
          matchedTerms: ['transport', 'miejscowości'],
        },
      ],
      similarNeeds: [
        {
          id: 'need-sim-1',
          excerpt: 'Trudności z dojazdem osób starszych do przychodni rejonowej w gminie wiejskiej',
          area: 'SERVICE_ACCESS',
        },
        {
          id: 'need-sim-2',
          excerpt: 'Brak zorganizowanego transportu medycznego dla osób z niepełnosprawnościami',
          area: 'AGING',
        },
      ],
      noGoodMatch: false,
    })
  }),

  http.put('/api/matches/:needId/feedback', async () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // Notifications
  http.get('/api/notifications', () => {
    return HttpResponse.json({
      items: [
        {
          id: 'notif-1',
          title: 'Nowe dopasowanie',
          message: 'Pojawiła się nowa innowacja pasująca do Twojego wyzwania.',
          read: false,
          createdAt: new Date().toISOString(),
        },
      ],
      page: 0,
      size: 10,
      total: 1,
    })
  }),
]
