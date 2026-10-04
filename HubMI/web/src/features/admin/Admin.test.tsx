import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AdminSummaryOverview } from './AdminSummaryOverview'
import { IdeasModerationQueue } from './IdeasModerationQueue'
import { TestRequestsModerationQueue } from './TestRequestsModerationQueue'
import { FeedbackModerationQueue } from './FeedbackModerationQueue'
import { MaterialsCrud } from './MaterialsCrud'
import { TrendsDashboard } from './TrendsDashboard'
import { ContentManagement } from './ContentManagement'
import { pluralizeSprawy } from './constants'
import { AdminPage } from '../../pages/AdminPage'
import { hubApi } from '../../api/hubApi'
import {
  AdminFeedbackJs,
  AdminSummaryJs,
  AdminTestRequestJs,
  ApiErrorJs,
  ApiResult,
  AreaTrendJs,
  IdeaJs,
  MaterialJs,
  MonthlyTrendPointJs,
  MunicipalityTrendJs,
  PageJs,
  TrendsJs,
} from 'hubmi-client'
import { axe } from 'vitest-axe'

let mockAuth = {
  ready: true,
  authenticated: true,
  username: 'admin.rops',
  roles: ['admin'],
  hasRole: (role: string) => mockAuth.roles.includes(role),
  login: vi.fn(),
  logout: vi.fn(),
}

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => mockAuth,
}))

function renderWithProviders(ui: React.ReactElement, initialEntries = ['/']) {
  return render(
    <MantineProvider>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </MantineProvider>,
  )
}

const mockSummary: AdminSummaryJs = new AdminSummaryJs(
  3, // submittedIdeas
  2, // pendingTestRequests
  5, // unmatchedNeedsThisWeek
  1, // pendingThreads
)

const mockIdeasPage: PageJs<IdeaJs> = {
  items: [
    new IdeaJs(
      'idea-001',
      'Klub Seniora w Wieliczce',
      'Zajęcia integracyjne i rękodzieło dla osób 60+.',
      ['SENIORS'],
      'TESTED',
      'SUBMITTED',
      null,
      '2026-10-04T08:00:00Z',
    ),
    new IdeaJs(
      'idea-002',
      'Cyfrowe Wtorki dla Babci i Dziadka',
      'Warsztaty smartfonowe i obsługa e-recepty.',
      ['SENIORS', 'YOUTH'],
      'IMPLEMENTED',
      'ACCEPTED',
      'Wdrożone z sukcesem',
      '2026-10-01T12:00:00Z',
    ),
  ],
  total: 2,
  page: 0,
  size: 10,
}

const mockTestRequestsPage: PageJs<AdminTestRequestJs> = {
  items: [
    new AdminTestRequestJs(
      'test-req-1',
      'inno-001',
      'Sąsiad dla Seniora',
      'user-senior-42',
      'Chciałbym sprawdzić to rozwiązanie w naszym sołectwie.',
      'NEW',
      '2026-10-04T09:30:00Z',
      '2026-10-04T09:30:00Z',
    ),
  ],
  total: 1,
  page: 0,
  size: 10,
}

const mockFeedbackPage: PageJs<AdminFeedbackJs> = {
  items: [
    new AdminFeedbackJs(
      'fb-1',
      'inno-001',
      'Sąsiad dla Seniora',
      'user-senior-42',
      5,
      'Świetny program! Sąsiedzi bardzo chętnie pomagają.',
      'Warto dodać więcej szkoleń dla koordynatorów.',
      '2026-10-04T10:00:00Z',
      '2026-10-04T10:00:00Z',
    ),
  ],
  total: 1,
  page: 0,
  size: 10,
}

const mockMaterialsPage: PageJs<MaterialJs> = {
  items: [
    new MaterialJs(
      'mat-1',
      'Film instruktażowy: obsługa platformy dla seniora',
      'Wideo pokazujące krok po kroku rejestrację (z napisami i transkrypcją WCAG)',
      'VIDEO',
      'https://youtube.com/watch?v=123',
      ['AGING'],
    ),
  ],
  total: 1,
  page: 0,
  size: 10,
}

const mockTrends: TrendsJs = new TrendsJs(
  [
    new AreaTrendJs('AGING', 42, 35),
    new AreaTrendJs('LONELINESS', 31, 28),
    new AreaTrendJs('MENTAL_HEALTH', 27, 19),
    new AreaTrendJs('SERVICE_ACCESS', 18, 20),
    new AreaTrendJs('DIGITAL_EXCLUSION', 15, 9),
    new AreaTrendJs('COORDINATION', 9, 10),
    new AreaTrendJs('DEPOPULATION', 7, 4),
  ],
  [
    new MunicipalityTrendJs('Kraków', 38),
    new MunicipalityTrendJs('Tarnów', 17),
    new MunicipalityTrendJs('Nowy Sącz', 14),
    new MunicipalityTrendJs('Wieliczka', 11),
    new MunicipalityTrendJs('Oświęcim', 9),
    new MunicipalityTrendJs('Zakopane', 6),
  ],
  22,
  [
    new MonthlyTrendPointJs('2026-05', 14),
    new MonthlyTrendPointJs('2026-06', 19),
    new MonthlyTrendPointJs('2026-07', 24),
    new MonthlyTrendPointJs('2026-08', 28),
    new MonthlyTrendPointJs('2026-09', 31),
    new MonthlyTrendPointJs('2026-10', 33),
  ],
  [
    'opieka wytchnieniowa',
    'tłumacz migowy',
    'asystent osoby niesamodzielnej',
    'transport door-to-door',
    'psycholog dziecięcy weekend',
  ],
  3,
)

describe('Panel administratora: moderacja i zarządzanie treścią (FE-08)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    mockAuth = {
      ready: true,
      authenticated: true,
      username: 'admin.rops',
      roles: ['admin'],
      hasRole: (role: string) => mockAuth.roles.includes(role),
      login: vi.fn(),
      logout: vi.fn(),
    }
  })

  describe('Dostęp i ochrona uprawnień', () => {
    it('blokuje dostęp i wyświetla błąd 403 dla użytkownika bez roli admin', () => {
      mockAuth.roles = ['user']

      renderWithProviders(<AdminPage />)

      expect(screen.getByRole('heading', { name: 'Brak uprawnień (403)' })).toBeInTheDocument()
      expect(screen.queryByText('Panel administratora ROPS')).not.toBeInTheDocument()
    })

    it('wyświetla wskaźnik strefy administratora ROPS dla zalogowanego admina', async () => {
      vi.spyOn(hubApi.admin, 'summary').mockResolvedValue(new ApiResult(mockSummary, null))

      renderWithProviders(<AdminPage />)

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Panel administratora ROPS' })).toBeInTheDocument()
      })
      expect(screen.getByText(/Strefa administratora – Regionalny Ośrodek Polityki Społecznej/i)).toBeInTheDocument()
    })
  })

  describe('Widok Wymaga uwagi i liczniki (AdminSummaryOverview)', () => {
    it('prezentuje 4 duże liczniki operacyjne ROPS i podsumowanie spraw pilnych', async () => {
      vi.spyOn(hubApi.admin, 'summary').mockResolvedValue(new ApiResult(mockSummary, null))

      renderWithProviders(<AdminSummaryOverview onNavigateTab={vi.fn()} />)

      await waitFor(() => {
        expect(screen.getByText('Zadania wymagające uwagi')).toBeInTheDocument()
      })

      // Counters check
      expect(screen.getByText('Nowe pomysły')).toBeInTheDocument()
      expect(screen.getByText('3')).toBeInTheDocument()

      expect(screen.getByText('Zgłoszenia do testów')).toBeInTheDocument()
      expect(screen.getByText('2')).toBeInTheDocument()

      expect(screen.getByText('Wiadomości ROPS')).toBeInTheDocument()
      expect(screen.getByText('1')).toBeInTheDocument()

      expect(screen.getByText('Luki w bazie (ten tydz.)')).toBeInTheDocument()
      expect(screen.getByText('5')).toBeInTheDocument()
    })

    it('poprawnie odmienia słowo "sprawa oczekująca" w języku polskim dla różnych liczebników', () => {
      expect(pluralizeSprawy(1).fullPhrase).toBe('1 sprawę oczekującą')
      expect(pluralizeSprawy(2).fullPhrase).toBe('2 sprawy oczekujące')
      expect(pluralizeSprawy(4).fullPhrase).toBe('4 sprawy oczekujące')
      expect(pluralizeSprawy(5).fullPhrase).toBe('5 spraw oczekujących')
      expect(pluralizeSprawy(12).fullPhrase).toBe('12 spraw oczekujących')
      expect(pluralizeSprawy(22).fullPhrase).toBe('22 sprawy oczekujące')
      expect(pluralizeSprawy(25).fullPhrase).toBe('25 spraw oczekujących')
      expect(pluralizeSprawy(112).fullPhrase).toBe('112 spraw oczekujących')
    })

    it('wyświetla poprawną odmianę gramatyczną w banerze dla 4 spraw oczekujących', async () => {
      const summaryWith4: AdminSummaryJs = new AdminSummaryJs(
        2, // submittedIdeas
        1, // pendingTestRequests
        0, // unmatchedNeedsThisWeek
        1, // pendingThreads
      )
      vi.spyOn(hubApi.admin, 'summary').mockResolvedValue(new ApiResult(summaryWith4, null))

      renderWithProviders(<AdminSummaryOverview onNavigateTab={vi.fn()} />)

      await waitFor(() => {
        expect(screen.getByText('Zadania wymagające uwagi')).toBeInTheDocument()
      })

      // 2 + 1 + 1 = 4 -> "4 sprawy oczekujące"
      expect(screen.getByText(/sprawy oczekujące na reakcję pracownika ROPS/i)).toBeInTheDocument()
    })
  })

  describe('Kolejka moderacji pomysłów (IdeasModerationQueue)', () => {
    it('renderuje dostępną tabelę z nagłówkami th[scope=col] i listą pomysłów', async () => {
      vi.spyOn(hubApi.admin, 'ideas').mockResolvedValue(new ApiResult(mockIdeasPage, null))

      renderWithProviders(<IdeasModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      // Semantic table header check
      const headers = screen.getAllByRole('columnheader')
      expect(headers.length).toBeGreaterThanOrEqual(4)
      expect(screen.getByText('Tytuł pomysłu i istota')).toBeInTheDocument()
      expect(screen.getByText('Zgłoszony (Nowy)')).toBeInTheDocument()
      expect(screen.getByText('Zaakceptowany')).toBeInTheDocument()
      expect(screen.getAllByText('Seniorzy i osoby starsze').length).toBeGreaterThanOrEqual(1)
    })

    it('zmienia status pomysłu i wysyła decyzję z komentarzem', async () => {
      vi.spyOn(hubApi.admin, 'ideas').mockResolvedValue(new ApiResult(mockIdeasPage, null))
      const updateSpy = vi.spyOn(hubApi.admin, 'updateIdeaStatus').mockResolvedValue(
        new ApiResult(mockIdeasPage.items[0], null),
      )

      renderWithProviders(<IdeasModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      // Open drawer
      const changeStatusBtns = screen.getAllByRole('button', { name: /Zmień status/i })
      fireEvent.click(changeStatusBtns[0])

      expect(screen.getByText('Weryfikacja i zmiana statusu pomysłu')).toBeInTheDocument()

      // Select ACCEPTED status
      const acceptedRadio = screen.getByLabelText(/Zaakceptowany/i)
      fireEvent.click(acceptedRadio)

      const commentInput = screen.getByLabelText(/Komentarz administratora dla autora pomysłu/i)
      fireEvent.change(commentInput, { target: { value: 'Projekt spełnia wszystkie kryteria inkubacji.' } })

      const saveBtn = screen.getByRole('button', { name: 'Zapisz decyzję' })
      fireEvent.click(saveBtn)

      await waitFor(() => {
        expect(updateSpy).toHaveBeenCalledWith(
          'idea-001',
          'ACCEPTED',
          'Projekt spełnia wszystkie kryteria inkubacji.',
        )
      })
    })

    it('obsługuje konflikt 409 przy równoległej zmianie statusu', async () => {
      vi.spyOn(hubApi.admin, 'ideas').mockResolvedValue(new ApiResult(mockIdeasPage, null))
      vi.spyOn(hubApi.admin, 'updateIdeaStatus').mockResolvedValue(
        new ApiResult<IdeaJs>(
          null as unknown as IdeaJs,
          new ApiErrorJs(409, 'CONFLICT', 'Conflict in status'),
        ),
      )

      renderWithProviders(<IdeasModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      const changeStatusBtns = screen.getAllByRole('button', { name: /Zmień status/i })
      fireEvent.click(changeStatusBtns[0])

      const acceptedRadio = screen.getByLabelText(/Zaakceptowany/i)
      fireEvent.click(acceptedRadio)

      const saveBtn = screen.getByRole('button', { name: 'Zapisz decyzję' })
      fireEvent.click(saveBtn)

      await waitFor(() => {
        expect(screen.getByText('Ktoś już zmienił ten status — odśwież listę pomysłów.')).toBeInTheDocument()
      })
    })

    it('wymaga podania uzasadnienia przy odrzuceniu pomysłu', async () => {
      vi.spyOn(hubApi.admin, 'ideas').mockResolvedValue(new ApiResult(mockIdeasPage, null))

      renderWithProviders(<IdeasModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      const changeStatusBtns = screen.getAllByRole('button', { name: /Zmień status/i })
      fireEvent.click(changeStatusBtns[0])

      // Select REJECTED
      const rejectedRadio = screen.getByLabelText(/Odrzucony/i)
      fireEvent.click(rejectedRadio)

      // Clear comment and click save
      const commentInput = screen.getByLabelText(/Komentarz administratora dla autora pomysłu/i)
      fireEvent.change(commentInput, { target: { value: '' } })

      const saveBtn = screen.getByRole('button', { name: 'Zapisz decyzję' })
      fireEvent.click(saveBtn)

      expect(screen.getByText('Przy odrzuceniu pomysłu uzasadnienie dla autora jest wymagane.')).toBeInTheDocument()
    })
  })

  describe('Zgłoszenia do testów innowacji (TestRequestsModerationQueue)', () => {
    it('wyświetla oczekujące zgłoszenia testerów i pozwala zaakceptować wniosek', async () => {
      vi.spyOn(hubApi.admin, 'testRequests').mockResolvedValue(
        new ApiResult(mockTestRequestsPage, null),
      )
      const decideSpy = vi.spyOn(hubApi.admin, 'decideTestRequest').mockResolvedValue(
        new ApiResult(mockTestRequestsPage.items[0], null),
      )

      renderWithProviders(<TestRequestsModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Sąsiad dla Seniora')).toBeInTheDocument()
        expect(screen.getByText('user-senior-42')).toBeInTheDocument()
      })

      // Click "Zaakceptuj"
      const acceptBtn = screen.getByRole('button', { name: 'Zaakceptuj' })
      fireEvent.click(acceptBtn)

      // Confirmation modal
      expect(screen.getByText('Potwierdzenie decyzji o zgłoszeniu do testów')).toBeInTheDocument()

      const confirmBtn = screen.getByRole('button', { name: 'Potwierdzam akceptację' })
      fireEvent.click(confirmBtn)

      await waitFor(() => {
        expect(decideSpy).toHaveBeenCalledWith('test-req-1', 'ACCEPTED')
      })
    })

    it('otwiera modal z pełną treścią notatki po kliknięciu "Zobacz całe uzasadnienie"', async () => {
      vi.spyOn(hubApi.admin, 'testRequests').mockResolvedValue(
        new ApiResult(mockTestRequestsPage, null),
      )

      renderWithProviders(<TestRequestsModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Sąsiad dla Seniora')).toBeInTheDocument()
      })

      const openNoteBtn = screen.getByRole('button', { name: /Zobacz całe uzasadnienie/i })
      fireEvent.click(openNoteBtn)

      expect(screen.getByText('Uzasadnienie zgłoszenia do testów')).toBeInTheDocument()
      expect(screen.getByText('Pełna treść uzasadnienia od testera:')).toBeInTheDocument()
      expect(screen.getAllByText('Chciałbym sprawdzić to rozwiązanie w naszym sołectwie.').length).toBe(2)
    })
  })

  describe('Opinie i recenzje testerów (FeedbackModerationQueue)', () => {
    it('prezentuje oceny, komentarze i sugestie usprawnień', async () => {
      vi.spyOn(hubApi.admin, 'feedback').mockResolvedValue(
        new ApiResult(mockFeedbackPage, null),
      )

      renderWithProviders(<FeedbackModerationQueue />)

      await waitFor(() => {
        expect(screen.getByText('Sąsiad dla Seniora')).toBeInTheDocument()
      })

      expect(screen.getByText('5 – Znakomicie')).toBeInTheDocument()
      expect(screen.getByText(/Świetny program! Sąsiedzi bardzo chętnie pomagają./i)).toBeInTheDocument()
      expect(screen.getByText(/Warto dodać więcej szkoleń dla koordynatorów./i)).toBeInTheDocument()
    })
  })

  describe('Materiały i wymóg transkrypcji WCAG dla wideo (MaterialsCrud)', () => {
    it('wymaga zaznaczenia dostępności (napisy/transkrypcja) przy dodawaniu materiału wideo', async () => {
      vi.spyOn(hubApi.materials, 'list').mockResolvedValue(
        new ApiResult(mockMaterialsPage, null),
      )
      const createSpy = vi.spyOn(hubApi.materials, 'create').mockResolvedValue(
        new ApiResult(mockMaterialsPage.items[0], null),
      )

      renderWithProviders(<MaterialsCrud />)

      await waitFor(() => {
        expect(screen.getByText('Film instruktażowy: obsługa platformy dla seniora')).toBeInTheDocument()
      })

      const addBtn = screen.getByRole('button', { name: 'Dodaj nowy materiał' })
      fireEvent.click(addBtn)

      const titleInput = screen.getByLabelText(/Tytuł publikacji/i)
      const urlInput = screen.getByLabelText(/Adres URL do pobrania lub odtworzenia/i)
      const descInput = screen.getByLabelText(/Opis zawartości materiału/i)

      fireEvent.change(titleInput, { target: { value: 'Nowy film wideo dla seniora' } })
      fireEvent.change(urlInput, { target: { value: 'https://youtube.com/watch?v=nowy' } })
      fireEvent.change(descInput, { target: { value: 'Opis nowego filmu' } })

      // Select VIDEO format
      const typeCombobox = screen.getByRole('combobox', { name: /Typ formatu publikacji/i })
      fireEvent.click(typeCombobox)
      const videoOption = screen.getByRole('option', { name: /Materiał wideo/i })
      fireEvent.click(videoOption)

      const submitBtn = screen.getByRole('button', { name: 'Utwórz materiał' })
      fireEvent.click(submitBtn)

      // Should fail WCAG validation because videoHasSubtitles is unchecked
      expect(
        screen.getByText(/Wymóg dostępności WCAG: Dla materiałów wideo wymagane jest zapewnienie napisów/i),
      ).toBeInTheDocument()

      // Now check accessibility checkbox
      const wcagCheckbox = screen.getByLabelText(/Zapewniono napisy lub transkrypcję tekstową/i)
      fireEvent.click(wcagCheckbox)

      fireEvent.click(submitBtn)

      await waitFor(() => {
        expect(createSpy).toHaveBeenCalled()
      })
    })

    it('formularz dodawania materiału rozpoczyna się bez zaznaczonego domyślnego obszaru', async () => {
      vi.spyOn(hubApi.materials, 'list').mockResolvedValue(
        new ApiResult(mockMaterialsPage, null),
      )

      renderWithProviders(<MaterialsCrud />)

      await waitFor(() => {
        expect(screen.getByText('Film instruktażowy: obsługa platformy dla seniora')).toBeInTheDocument()
      })

      const addBtn = screen.getByRole('button', { name: 'Dodaj nowy materiał' })
      fireEvent.click(addBtn)

      // The areas MultiSelect should not have any preselected pills
      const areasInputs = screen.getAllByLabelText(/Obszary tematyczne/i)
      expect(areasInputs[0]).toHaveValue('')
      expect(document.querySelector('.mantine-MultiSelect-pill')).toBeNull()
    })
  })

  describe('Zarządzanie treścią ROPS (ContentManagement)', () => {
    it('wyświetla pełne, nieucięte zakładki dla innowacji, wyzwań i materiałów edukacyjnych', async () => {
      vi.spyOn(hubApi.innovations, 'list').mockResolvedValue(
        new ApiResult({ items: [], total: 0, page: 0, size: 10 }, null),
      )
      vi.spyOn(hubApi.challenges, 'list').mockResolvedValue(
        new ApiResult({ items: [], total: 0, page: 0, size: 10 }, null),
      )
      vi.spyOn(hubApi.materials, 'list').mockResolvedValue(
        new ApiResult({ items: [], total: 0, page: 0, size: 10 }, null),
      )

      renderWithProviders(<ContentManagement />)

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Innowacje społeczne/i })).toBeInTheDocument()
      })
      expect(screen.getByRole('tab', { name: /Wyzwania Małopolski/i })).toBeInTheDocument()
      expect(
        screen.getByRole('tab', { name: /Materiały i publikacje edukacyjne/i }),
      ).toBeInTheDocument()
    })
  })

  describe('Trendy i diagnoza (TrendsDashboard - FE-09)', () => {
    it('prezentuje pełny dashboard trendów z kluczowymi KPI, obszarami, osią czasu i gminami', async () => {
      vi.spyOn(hubApi.admin, 'trends').mockResolvedValue(new ApiResult(mockTrends, null))

      renderWithProviders(<TrendsDashboard />)

      await waitFor(() => {
        expect(
          screen.getByRole('heading', { level: 2, name: /Analiza trendów i diagnoza potrzeb społecznych/i }),
        ).toBeInTheDocument()
      })

      // KPI cards
      expect(screen.getByText('149')).toBeInTheDocument()
      expect(screen.getByText('22')).toBeInTheDocument()
      expect(screen.getByText('6')).toBeInTheDocument()

      // Areas (matching SOCIAL_AREA_NAMES)
      expect(screen.getAllByText('Wsparcie seniorów i osób starszych').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Przeciwdziałanie samotności i integracja').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Zdrowie psychiczne i samopoczucie').length).toBeGreaterThan(0)
      expect(screen.getByText('+20%')).toBeInTheDocument()

      // Timeline
      expect(screen.getAllByText('2026-05').length).toBeGreaterThan(0)
      expect(screen.getAllByText('2026-10').length).toBeGreaterThan(0)

      // Municipalities
      expect(screen.getAllByText('Kraków').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Tarnów').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Wieliczka').length).toBeGreaterThan(0)

      // Top unmatched terms
      expect(screen.getByText(/opieka wytchnieniowa/i)).toBeInTheDocument()
      expect(screen.getByText(/tłumacz migowy/i)).toBeInTheDocument()
    })

    it('pozwala przełączyć widok na tryb dostępności (tylko tabele)', async () => {
      vi.spyOn(hubApi.admin, 'trends').mockResolvedValue(new ApiResult(mockTrends, null))

      renderWithProviders(<TrendsDashboard />)

      await waitFor(() => {
        expect(
          screen.getByRole('heading', { level: 2, name: /Analiza trendów i diagnoza potrzeb społecznych/i }),
        ).toBeInTheDocument()
      })

      const tableModeBtn = screen.getByText('Tylko tabela')
      fireEvent.click(tableModeBtn)

      expect(
        screen.getByRole('table', { name: /Tabela potrzeb według obszarów społecznych/i }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('table', { name: /Tabela dynamiki zgłoszeń w czasie/i }),
      ).toBeInTheDocument()
    })

    it('reaguje na zmianę horyzontu czasowego (parametr months)', async () => {
      const trendsSpy = vi.spyOn(hubApi.admin, 'trends').mockResolvedValue(new ApiResult(mockTrends, null))

      renderWithProviders(<TrendsDashboard />)

      await waitFor(() => {
        expect(trendsSpy).toHaveBeenCalledWith(6)
      })

      const periodSelect = screen.getByRole('combobox', { name: /Okres analizy/i })
      fireEvent.click(periodSelect)

      const option12 = screen.getByRole('option', { name: /Ostatni rok/i, hidden: true })
      fireEvent.click(option12)

      await waitFor(() => {
        expect(trendsSpy).toHaveBeenCalledWith(12)
      })
    })

    it('umożliwia szybkie przejście do formularza dodawania nowej innowacji dla wykrytej luki', async () => {
      vi.spyOn(hubApi.admin, 'trends').mockResolvedValue(new ApiResult(mockTrends, null))
      const onNavigate = vi.fn()

      renderWithProviders(<TrendsDashboard onNavigateTab={onNavigate} />)

      await waitFor(() => {
        expect(screen.getByText(/opieka wytchnieniowa/i)).toBeInTheDocument()
      })

      const addButtons = screen.getAllByRole('button', { name: /Dodaj nową innowację odpowiadającą na potrzebę/i })
      fireEvent.click(addButtons[0])

      expect(onNavigate).toHaveBeenCalledWith('tresci', 'innowacje')
    })

    it('obsługuje błąd API i umożliwia ponowienie zapytania', async () => {
      vi.spyOn(hubApi.admin, 'trends')
        .mockResolvedValueOnce(new ApiResult<TrendsJs>(null, new ApiErrorJs(500, 'ERROR', 'Błąd pobierania trendów')))
        .mockResolvedValueOnce(new ApiResult(mockTrends, null))

      renderWithProviders(<TrendsDashboard />)

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
      })

      const retryBtn = screen.getByRole('button', { name: /Spróbuj ponownie/i })
      fireEvent.click(retryBtn)

      await waitFor(() => {
        expect(screen.getAllByText('Wsparcie seniorów i osób starszych')[0]).toBeInTheDocument()
      })
    })

    it('jest zgodny z WCAG 2.1 AA (brak naruszeń dostępności axe)', async () => {
      vi.spyOn(hubApi.admin, 'trends').mockResolvedValue(new ApiResult(mockTrends, null))

      const { container } = renderWithProviders(<TrendsDashboard />)

      await waitFor(() => {
        expect(screen.getAllByText('Wsparcie seniorów i osób starszych')[0]).toBeInTheDocument()
      })

      expect(await axe(container)).toHaveNoViolations()
    })

    it('przycisk w AdminSummaryOverview prowadzi bezpośrednio do zakładki trendy', async () => {
      vi.spyOn(hubApi.admin, 'summary').mockResolvedValue(new ApiResult(mockSummary, null))
      const onNavigate = vi.fn()

      renderWithProviders(<AdminSummaryOverview onNavigateTab={onNavigate} />)

      await waitFor(() => {
        expect(screen.getByText(/Luki w bazie/i)).toBeInTheDocument()
      })

      const trendsBtn = screen.getByRole('button', { name: 'Analiza trendów i luk' })
      fireEvent.click(trendsBtn)

      expect(onNavigate).toHaveBeenCalledWith('trendy')
    })

    it('zakładka Trendy i diagnoza jest widoczna i przełącza widok w AdminPage', async () => {
      vi.spyOn(hubApi.admin, 'summary').mockResolvedValue(new ApiResult(mockSummary, null))
      vi.spyOn(hubApi.admin, 'trends').mockResolvedValue(new ApiResult(mockTrends, null))

      renderWithProviders(<AdminPage />)

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Trendy i diagnoza/i })).toBeInTheDocument()
      })

      fireEvent.click(screen.getByRole('tab', { name: /Trendy i diagnoza/i }))

      await waitFor(() => {
        expect(
          screen.getByRole('heading', { level: 2, name: /Analiza trendów i diagnoza potrzeb społecznych/i }),
        ).toBeInTheDocument()
      })
    })
  })
})

