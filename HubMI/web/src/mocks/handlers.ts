import { http, HttpResponse } from 'msw'

const mockInnovations = [
  {
    id: 'inno-001',
    title: 'Inteligentny asystent komunikacji dla seniorów',
    summary: 'Aplikacja mobilna upraszczająca kontakt z rodziną i służbami medycznymi.',
    description: 'Szczegółowy opis rozwiązania wspomagającego codzienne funkcjonowanie osób starszych.',
    area: 'Dostępność cyfrowa',
    targetGroup: 'Seniorzy',
    maturityLevel: 'TRL 7 - prototyp sprawdzony w warunkach operacyjnych',
    institution: 'Politechnika Warszawska',
    contactEmail: 'innowacje@pw.edu.pl',
    createdAt: '2026-03-01T10:00:00Z',
    updatedAt: '2026-03-15T14:30:00Z',
  },
  {
    id: 'inno-002',
    title: 'Ekomateriały biodegradowalne do opakowań leków',
    summary: 'Biodegradowalny kompozyt z surowców odnawialnych redukujący ślad węglowy w farmacji.',
    description: 'Zaawansowany polimer celulozowy opracowany w standardzie cleanroom.',
    area: 'Zielona transformacja',
    targetGroup: 'Przemysł farmaceutyczny',
    maturityLevel: 'TRL 6 - technologia zademonstrowana',
    institution: 'Instytut Chemii Przemysłowej',
    contactEmail: 'kontakt@ichp.pl',
    createdAt: '2026-03-10T12:00:00Z',
    updatedAt: '2026-03-20T09:15:00Z',
  },
]

const mockChallenges = [
  {
    id: 'chal-001',
    title: 'Cyfryzacja i optymalizacja tras transportu publicznego w gminach wiejskich',
    description: 'Poszukujemy algorytmu optymalizującego zapotrzebowanie na transport na żądanie.',
    area: 'Transport i mobilność',
    organization: 'Związek Gmin Wiejskich',
    deadline: '2026-11-30T23:59:59Z',
    budget: '150 000 PLN',
    createdAt: '2026-02-15T08:00:00Z',
  },
  {
    id: 'chal-002',
    title: 'Narzędzie AI do automatycznego generowania audytów WCAG 2.1',
    description: 'Zapotrzebowanie na otwarte narzędzie wspierające urzędy w audycie dostępności serwisów.',
    area: 'Dostępność cyfrowa',
    organization: 'Ministerstwo Cyfryzacji',
    deadline: '2026-12-15T23:59:59Z',
    budget: '300 000 PLN',
    createdAt: '2026-03-01T09:00:00Z',
  },
]

const mockMaterials = [
  {
    id: 'mat-001',
    title: 'Poradnik wdrażania standardu WCAG 2.1 AA w jednostkach publicznych',
    description: 'Kompletny zbiór dobrych praktyk, szablonów i checklist dla audytorów i programistów.',
    type: 'Przewodnik',
    area: 'Dostępność cyfrowa',
    fileUrl: '/files/poradnik-wcag.pdf',
    fileSize: '4.2 MB',
    createdAt: '2026-01-10T10:00:00Z',
  },
]

export const handlers = [
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

    return HttpResponse.json({
      items: mockMaterials.slice(page * size, (page + 1) * size),
      page,
      size,
      total: mockMaterials.length,
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
