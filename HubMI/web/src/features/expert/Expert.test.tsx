import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { hubApi } from '../../api/hubApi'
import { ExpertPage } from '../../pages/ExpertPage'
import { ExpertPanel } from './ExpertPanel'
import { ExpertIdeasReview } from './ExpertIdeasReview'
import { ExpertConsultationsQueue } from './ExpertConsultationsQueue'
import { ExpertAdvisoryHub } from './ExpertAdvisoryHub'
import {
  ApiResult,
  IdeaJs,
  PageJs,
  ThreadJs,
} from 'hubmi-client'

let mockAuth = {
  ready: true,
  authenticated: true,
  username: 'piotr.ekspert',
  roles: ['expert'],
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

const mockIdeasPage: PageJs<IdeaJs> = {
  items: [
    new IdeaJs(
      'idea-001',
      'Mobilny Klub Seniora w Wieliczce',
      'Zajęcia integracyjne i rękodzieło dla osób 60+.',
      ['SENIORS'],
      'IDEA',
      'SUBMITTED',
      null,
      '2026-10-01T10:00:00Z',
    ),
    new IdeaJs(
      'idea-002',
      'Cyfrowy Asystent dla Osób Niesłyszących',
      'Aplikacja tłumacząca mowę na polski język migowy.',
      ['PEOPLE_WITH_DISABILITIES'],
      'PROTOTYPE',
      'IN_REVIEW',
      null,
      '2026-10-02T12:00:00Z',
    ),
  ],
  page: 0,
  size: 10,
  total: 2,
}

const mockThreadsPage: PageJs<ThreadJs> = {
  items: [
    new ThreadJs(
      'thread-001',
      'Pytanie o grupę docelową innowacji',
      'idea-001',
      '2026-10-03T14:30:00Z',
      true,
    ),
    new ThreadJs(
      'thread-002',
      'Wdrożenie klubu seniora na terenach wiejskich',
      null,
      '2026-10-02T09:15:00Z',
      false,
    ),
  ],
  page: 0,
  size: 10,
  total: 2,
}

describe('Strefa Eksperta Merytorycznego', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    mockAuth = {
      ready: true,
      authenticated: true,
      username: 'piotr.ekspert',
      roles: ['expert'],
      hasRole: (role: string) => mockAuth.roles.includes(role),
      login: vi.fn(),
      logout: vi.fn(),
    }

    vi.spyOn(hubApi.admin, 'ideas').mockResolvedValue(
      new ApiResult<PageJs<IdeaJs>>(mockIdeasPage, null),
    )
    vi.spyOn(hubApi.threads, 'list').mockResolvedValue(
      new ApiResult<PageJs<ThreadJs>>(mockThreadsPage, null),
    )
  })

  describe('Dostęp i ochrona uprawnień', () => {
    it('blokuje dostęp i wyświetla błąd 403 dla użytkownika ze zwykłą rolą user', () => {
      mockAuth.roles = ['user']
      renderWithProviders(<ExpertPage />)

      expect(screen.getByTestId('forbidden-page')).toBeInTheDocument()
      expect(screen.getByText(/Brak uprawnień \(403\)/i)).toBeInTheDocument()
    })

    it('udostępnia widok dla użytkownika z rolą expert', async () => {
      mockAuth.roles = ['expert']
      renderWithProviders(<ExpertPage />)

      await waitFor(() => {
        expect(screen.getByText('Strefa Eksperta Merytorycznego')).toBeInTheDocument()
      })
    })

    it('udostępnia widok również dla administratora ROPS', async () => {
      mockAuth.roles = ['admin']
      renderWithProviders(<ExpertPage />)

      await waitFor(() => {
        expect(screen.getByText('Strefa Eksperta Merytorycznego')).toBeInTheDocument()
      })
    })
  })

  describe('Główny panel i metryki (ExpertPanel)', () => {
    it('renderuje nagłówek, banner informacyjny i 3 kafelki metryk', async () => {
      renderWithProviders(<ExpertPanel />)

      expect(screen.getByText('Strefa Eksperta Merytorycznego')).toBeInTheDocument()
      expect(screen.getByText(/Zakres uprawnień eksperta merytorycznego/i)).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.getByText('Pomysły w bazie')).toBeInTheDocument()
        expect(screen.getByText('Wątki konsultacyjne')).toBeInTheDocument()
        expect(screen.getByText('Doradztwo dla JST')).toBeInTheDocument()
      })
    })

    it('przełącza zakładki panelu eksperta', async () => {
      renderWithProviders(<ExpertPanel />)

      await waitFor(() => {
        expect(screen.getByText('Mobilny Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      // Switch to consultations tab
      const consultationsTab = screen.getByRole('tab', { name: /Moje konsultacje i wątki/i })
      fireEvent.click(consultationsTab)

      await waitFor(() => {
        expect(screen.getByText('Pytanie o grupę docelową innowacji')).toBeInTheDocument()
        expect(screen.getByText('Wdrożenie klubu seniora na terenach wiejskich')).toBeInTheDocument()
      })

      // Switch to JST advisory tab
      const advisoryTab = screen.getByRole('tab', { name: /Doradztwo dla samorządów/i })
      fireEvent.click(advisoryTab)

      await waitFor(() => {
        expect(screen.getByText(/Standard wsparcia doradczego dla JST/i)).toBeInTheDocument()
        expect(screen.getByText(/Wyzwania Małopolski/i)).toBeInTheDocument()
      })
    })
  })

  describe('Pomysły i wnioski do zaopiniowania (ExpertIdeasReview)', () => {
    it('renderuje tabelę pomysłów z poprawnymi nagłówkami WCAG th[scope=col]', async () => {
      renderWithProviders(<ExpertIdeasReview />)

      await waitFor(() => {
        expect(screen.getByText('Mobilny Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      const headers = screen.getAllByRole('columnheader')
      expect(headers.length).toBeGreaterThanOrEqual(4)
      headers.forEach((h) => expect(h).toHaveAttribute('scope', 'col'))
    })

    it('otwiera drawer analizy merytorycznej i pozwala zapisać notatkę ekspercką', async () => {
      renderWithProviders(<ExpertIdeasReview />)

      await waitFor(() => {
        expect(screen.getByText('Mobilny Klub Seniora w Wieliczce')).toBeInTheDocument()
      })

      // Click "Zaopiniuj" button
      const reviewButtons = screen.getAllByRole('button', { name: /Zaopiniuj/i })
      fireEvent.click(reviewButtons[0])

      await waitFor(() => {
        expect(screen.getByText('Analiza merytoryczna pomysłu')).toBeInTheDocument()
        expect(screen.getByText('Istota pomysłu i opis rozwiązania')).toBeInTheDocument()
      })

      // Type expert note
      const textarea = screen.getByLabelText(/Treść notatki eksperckiej/i)
      fireEvent.change(textarea, { target: { value: 'Projekt wykazuje wysoki potencjał integracyjny dla seniorów 70+.' } })

      const saveButton = screen.getByRole('button', { name: /Zapisz notatkę/i })
      fireEvent.click(saveButton)

      await waitFor(() => {
        expect(screen.getByText('Notatka została zapisana!')).toBeInTheDocument()
      })

      expect(localStorage.getItem('hubmi_expert_note_idea-001')).toBe(
        'Projekt wykazuje wysoki potencjał integracyjny dla seniorów 70+.',
      )
    })
  })

  describe('Moje konsultacje i wątki (ExpertConsultationsQueue)', () => {
    it('wyświetla listę aktywnych wątków konsultacyjnych z oznaczeniem nowych wiadomości', async () => {
      renderWithProviders(<ExpertConsultationsQueue />)

      await waitFor(() => {
        expect(screen.getByText('Pytanie o grupę docelową innowacji')).toBeInTheDocument()
        expect(screen.getAllByText('Nowa wiadomość').length).toBeGreaterThanOrEqual(1)
        expect(screen.getByText('Przeczytane')).toBeInTheDocument()
      })
    })
  })

  describe('Doradztwo dla samorządów JST (ExpertAdvisoryHub)', () => {
    it('prezentuje standard wsparcia doradczego w 4 krokach', () => {
      renderWithProviders(<ExpertAdvisoryHub />)

      expect(screen.getByText(/Diagnoza potrzeb seniorów w gminie/i)).toBeInTheDocument()
      expect(screen.getByText(/Dobór gotowej innowacji z bazy/i)).toBeInTheDocument()
      expect(screen.getByText(/Montaż finansowy i nabory grantowe/i)).toBeInTheDocument()
      expect(screen.getByText(/Pilotaż i ewaluacja z testerami/i)).toBeInTheDocument()
    })
  })
})
