import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AdminSummaryOverview } from './AdminSummaryOverview'
import { IdeasModerationQueue } from './IdeasModerationQueue'
import { TestRequestsModerationQueue } from './TestRequestsModerationQueue'
import { FeedbackModerationQueue } from './FeedbackModerationQueue'
import { MaterialsCrud } from './MaterialsCrud'
import { AdminPage } from '../../pages/AdminPage'
import { hubApi } from '../../api/hubApi'
import {
  AdminFeedbackJs,
  AdminSummaryJs,
  AdminTestRequestJs,
  ApiErrorJs,
  ApiResult,
  IdeaJs,
  MaterialJs,
  PageJs,
} from 'hubmi-client'

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
  })
})
