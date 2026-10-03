import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { InnovationsListPage } from './InnovationsListPage'
import { InnovationDetailPage } from './InnovationDetailPage'
import { ChallengesListPage } from './ChallengesListPage'
import { MaterialsListPage } from './MaterialsListPage'
import { hubApi } from '../../api/hubApi'
import {
  ApiResult,
  ChallengeJs,
  InnovationJs,
  InnovationSummaryJs,
  MaterialJs,
  PageJs,
} from 'hubmi-client'

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    ready: true,
    authenticated: true,
    username: 'jan.kowalski',
    roles: ['user'],
    hasRole: () => false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}))

function renderWithProviders(ui: React.ReactElement, initialEntries = ['/']) {
  return render(
    <MantineProvider>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </MantineProvider>,
  )
}

const mockInnovationsPage: PageJs<InnovationSummaryJs> = {
  items: [
    new InnovationSummaryJs(
      'inno-001',
      'Sąsiad dla Seniora',
      'System mikro-wolontariatu sąsiedzkiego łączący osoby starsze z sąsiadami.',
      ['AGING', 'LONELINESS'],
      ['SENIORS', 'RESIDENTS'],
      'IMPLEMENTED',
    ),
    new InnovationSummaryJs(
      'inno-002',
      'Cyfrowy Przewodnik Pokoleń',
      'Warsztaty smartfonowe prowadzone przez młodzież dla seniorów.',
      ['DIGITAL_EXCLUSION', 'AGING'],
      ['SENIORS', 'YOUTH'],
      'TESTED',
    ),
  ],
  total: 2,
  page: 0,
  size: 10,
}

const mockInnovationDetail: InnovationJs = new InnovationJs(
  'inno-001',
  'Sąsiad dla Seniora',
  'System mikro-wolontariatu sąsiedzkiego łączący osoby starsze z sąsiadami.',
  'Dokładny opis działania innowacji w lokalnych społecznościach.',
  ['AGING', 'LONELINESS'],
  ['SENIORS', 'RESIDENTS'],
  'IMPLEMENTED',
  'Kraków - Podgórze',
  ['https://rops.krakow.pl/innowacje/sasiad.pdf', 'https://youtube.com/watch?v=wideo'],
  4.8,
  12,
  'Nowy model oparty na zaufaniu i koordynatorze.',
  'Izolacja seniorów w wielkich miastach.',
  'Osoby 65+ mieszkające samotnie.',
  'Wzrost poczucia bezpieczeństwa i wsparcia.',
  'Możliwość wdrożenia w każdej gminie.',
)

const mockChallengesPage: PageJs<ChallengeJs> = {
  items: [
    new ChallengeJs(
      'chal-001',
      'Transport na żądanie dla seniorów z sołectw podmiejskich',
      'Osoby starsze mieszkające w mniejszych sołectwach mają utrudniony dostęp do placówek ochrony zdrowia.',
      'AGING',
      ['Wieliczka', 'Niepołomice'],
    ),
  ],
  total: 1,
  page: 0,
  size: 10,
}

const mockMaterialsPage: PageJs<MaterialJs> = {
  items: [
    new MaterialJs(
      'mat-001',
      'Jak inkubować innowację społeczną – poradnik krok po kroku',
      'Kompleksowy przewodnik dla animatorów i samorządowców opisujący etapy innowacji.',
      'GUIDE',
      'https://rops.krakow.pl/innowacje/poradnik.pdf',
      ['COORDINATION', 'OTHER'],
    ),
  ],
  total: 1,
  page: 0,
  size: 10,
}

describe('Knowledge Base Feature (FE-04)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  describe('InnovationsListPage', () => {
    it('renders search input, filter selects, and fetched innovations', async () => {
      vi.spyOn(hubApi.innovations, 'list').mockResolvedValue(
        new ApiResult(mockInnovationsPage, null),
      )

      renderWithProviders(<InnovationsListPage />)

      expect(screen.getByText('Baza innowacji społecznych')).toBeInTheDocument()
      expect(screen.getByLabelText(/Wyszukaj innowację/i)).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.getByText('Sąsiad dla Seniora')).toBeInTheDocument()
        expect(screen.getByText('Cyfrowy Przewodnik Pokoleń')).toBeInTheDocument()
      })

      expect(screen.getByText('Gotowe i działające rozwiązanie')).toBeInTheDocument()
      expect(screen.getAllByText('Wsparcie seniorów i osób starszych').length).toBeGreaterThan(0)
    })

    it('submits search query and triggers API with query parameter', async () => {
      const listSpy = vi.spyOn(hubApi.innovations, 'list').mockResolvedValue(
        new ApiResult(mockInnovationsPage, null),
      )

      renderWithProviders(<InnovationsListPage />)

      const searchInput = screen.getByLabelText(/Wyszukaj innowację/i)
      fireEvent.change(searchInput, { target: { value: 'smartfon' } })

      const submitButton = screen.getByRole('button', { name: 'Szukaj' })
      fireEvent.click(submitButton)

      await waitFor(() => {
        expect(listSpy).toHaveBeenCalledWith('smartfon', undefined, undefined, 0, 10)
      })
    })

    it('renders empty state when no innovations match criteria', async () => {
      vi.spyOn(hubApi.innovations, 'list').mockResolvedValue(
        new ApiResult({ items: [], total: 0, page: 0, size: 10 } as PageJs<InnovationSummaryJs>, null),
      )

      renderWithProviders(<InnovationsListPage />)

      await waitFor(() => {
        expect(screen.getByText('Brak innowacji spełniających podane kryteria')).toBeInTheDocument()
      })
    })
  })

  describe('InnovationDetailPage', () => {
    it('renders innovation details, ROPS sections, and media list', async () => {
      vi.spyOn(hubApi.innovations, 'get').mockResolvedValue(
        new ApiResult(mockInnovationDetail, null),
      )

      renderWithProviders(
        <Routes>
          <Route path="/innowacje/:id" element={<InnovationDetailPage />} />
        </Routes>,
        ['/innowacje/inno-001'],
      )

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Sąsiad dla Seniora' })).toBeInTheDocument()
      })

      // ROPS Narrative Sections
      expect(screen.getByText('Innowacyjność rozwiązania')).toBeInTheDocument()
      expect(screen.getByText('Nowy model oparty na zaufaniu i koordynatorze.')).toBeInTheDocument()
      expect(screen.getByText('Diagnoza problemu')).toBeInTheDocument()
      expect(screen.getByText('Izolacja seniorów w wielkich miastach.')).toBeInTheDocument()

      // Media Section with WCAG video/caption note
      expect(screen.getByText('Materiały do pobrania i multimedia')).toBeInTheDocument()
      expect(screen.getByText('Materiał wideo (z napisami i transkrypcją)')).toBeInTheDocument()

      // Test Request button
      expect(screen.getByText('Chcę przetestować to rozwiązanie')).toBeInTheDocument()

      // Rating Section
      expect(screen.getByText('Opinie i oceny rozwiązania')).toBeInTheDocument()
    })
  })

  describe('ChallengesListPage', () => {
    it('renders challenges list with area badge and municipalities', async () => {
      vi.spyOn(hubApi.challenges, 'list').mockResolvedValue(
        new ApiResult(mockChallengesPage, null),
      )

      renderWithProviders(<ChallengesListPage />)

      expect(screen.getByText('Katalog wyzwań społecznych Małopolski')).toBeInTheDocument()

      await waitFor(() => {
        expect(
          screen.getByText('Transport na żądanie dla seniorów z sołectw podmiejskich'),
        ).toBeInTheDocument()
      })

      expect(screen.getAllByText('Wieliczka').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Niepołomice').length).toBeGreaterThan(0)
      expect(screen.getByText('Zobacz pasujące innowacje dla tego wyzwania')).toBeInTheDocument()
    })
  })

  describe('MaterialsListPage', () => {
    it('renders materials list with format badge and download action', async () => {
      vi.spyOn(hubApi.materials, 'list').mockResolvedValue(
        new ApiResult(mockMaterialsPage, null),
      )

      renderWithProviders(<MaterialsListPage />)

      expect(screen.getByText('Materiały i publikacje edukacyjne')).toBeInTheDocument()

      await waitFor(() => {
        expect(
          screen.getByText('Jak inkubować innowację społeczną – poradnik krok po kroku'),
        ).toBeInTheDocument()
      })

      expect(screen.getAllByText('Poradnik krok po kroku').length).toBeGreaterThan(0)
      expect(screen.getByText('Pobierz materiał (PDF)')).toBeInTheDocument()
    })

    it('re-fetches materials without infinite loading when submitting search with empty query', async () => {
      const listSpy = vi.spyOn(hubApi.materials, 'list').mockResolvedValue(
        new ApiResult(mockMaterialsPage, null),
      )

      renderWithProviders(<MaterialsListPage />)

      await waitFor(() => {
        expect(
          screen.getByText('Jak inkubować innowację społeczną – poradnik krok po kroku'),
        ).toBeInTheDocument()
      })

      // Click "Szukaj" with empty input
      const submitButton = screen.getByRole('button', { name: 'Szukaj' })
      fireEvent.click(submitButton)

      // It should re-fetch and NOT get stuck in loading
      await waitFor(() => {
        expect(listSpy).toHaveBeenCalledTimes(2)
      })

      expect(
        screen.getByText('Jak inkubować innowację społeczną – poradnik krok po kroku'),
      ).toBeInTheDocument()
    })
  })
})
