import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { MatchmakingForm } from './MatchmakingForm'
import { MatchCard } from './MatchCard'
import { MatchFeedback } from './MatchFeedback'
import { SimilarNeedsList } from './SimilarNeedsList'
import { MatchmakingView } from './MatchmakingView'
import { hubApi } from '../../api/hubApi'
import {
  ApiErrorJs,
  ApiResult,
  EmptyJs,
  InnovationSummaryJs,
  MatchJs,
  MatchResultJs,
} from 'hubmi-client'
import type { MatchItem } from './types'

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <MantineProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </MantineProvider>,
  )
}

const mockMatchItem: MatchItem = {
  score: 0.88,
  reasons: [
    'Innowacja bezpośrednio rozwiązuje problem komunikacji.',
    'Testy z seniorami wykazały 90% satysfakcji.',
  ],
  matchedTerms: ['senior', 'komunikacja'],
  innovation: {
    id: 'inno-101',
    title: 'Mobilny Asystent Seniora',
    summary: 'Proste narzędzie do kontaktu ze służbami.',
    areas: ['AGING', 'DIGITAL_EXCLUSION'],
    targetGroups: ['SENIORS'],
    stage: 'TESTED',
  },
}

describe('Matchmaking Feature', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('MatchmakingForm', () => {
    it('renders textarea, RODO warning, counter and sample queries', () => {
      renderWithProviders(<MatchmakingForm isLoading={false} onSubmit={vi.fn()} />)

      expect(screen.getByLabelText(/Opisz problem swoimi słowami/)).toBeInTheDocument()
      expect(screen.getByText(/Nie musisz podawać swoich danych osobowych/)).toBeInTheDocument()
      expect(screen.getByText(/0 \/ 2000 znaków/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Transport seniorów' })).toBeInTheDocument()
    })

    it('populates textarea when clicking a sample query button', () => {
      renderWithProviders(<MatchmakingForm isLoading={false} onSubmit={vi.fn()} />)

      const sampleBtn = screen.getByRole('button', { name: 'Transport seniorów' })
      fireEvent.click(sampleBtn)

      const textarea = screen.getByLabelText(/Opisz problem swoimi słowami/) as HTMLTextAreaElement
      expect(textarea.value).toContain('Brak transportu dla seniorów')
      expect(screen.getByText(new RegExp(`${textarea.value.length} \\/ 2000 znaków`))).toBeInTheDocument()
    })

    it('validates minimum description length', () => {
      const handleSubmit = vi.fn()
      renderWithProviders(<MatchmakingForm isLoading={false} onSubmit={handleSubmit} />)

      const textarea = screen.getByLabelText(/Opisz problem swoimi słowami/)
      fireEvent.change(textarea, { target: { value: 'test' } })

      const submitBtn = screen.getByRole('button', { name: /Wyszukaj sprawdzone rozwiązania/ })
      fireEvent.click(submitBtn)

      expect(screen.getByText(/Opis problemu jest zbyt krótki/)).toBeInTheDocument()
      expect(handleSubmit).not.toHaveBeenCalled()
    })

    it('submits valid input', () => {
      const handleSubmit = vi.fn()
      renderWithProviders(<MatchmakingForm isLoading={false} onSubmit={handleSubmit} />)

      const textarea = screen.getByLabelText(/Opisz problem swoimi słowami/)
      fireEvent.change(textarea, {
        target: { value: 'Seniorzy w naszej wsi nie mają jak dojechać do ośrodka zdrowia.' },
      })

      const submitBtn = screen.getByRole('button', { name: /Wyszukaj sprawdzone rozwiązania/ })
      fireEvent.click(submitBtn)

      expect(handleSubmit).toHaveBeenCalledWith(
        'Seniorzy w naszej wsi nie mają jak dojechać do ośrodka zdrowia.',
        undefined,
      )
    })
  })

  describe('MatchCard', () => {
    it('renders innovation details, reasons, quality badge, and marks', () => {
      renderWithProviders(<MatchCard match={mockMatchItem} />)

      expect(screen.getByRole('heading', { level: 3, name: 'Mobilny Asystent Seniora' })).toBeInTheDocument()
      expect(screen.getByText('Bardzo wysoka zgodność z problemem')).toBeInTheDocument()
      expect(screen.getByText('Przetestowane z mieszkańcami')).toBeInTheDocument()
      expect(screen.getByText('Innowacja bezpośrednio rozwiązuje problem komunikacji.')).toBeInTheDocument()

      const marks = screen.getAllByText(/senior|komunikacja/)
      expect(marks.length).toBeGreaterThan(0)
    })
  })

  describe('MatchFeedback', () => {
    it('sends feedback and confirms in aria-live region', async () => {
      const sendFeedbackSpy = vi.spyOn(hubApi.matches, 'sendFeedback').mockResolvedValue(
        new ApiResult(new EmptyJs(), null),
      )

      renderWithProviders(<MatchFeedback needId="need-123" />)

      const yesBtn = screen.getByRole('button', { name: 'Tak, pomocne' })
      fireEvent.click(yesBtn)

      expect(sendFeedbackSpy).toHaveBeenCalledWith('need-123', true)
      await waitFor(() => {
        expect(screen.getByText(/Dziękujemy za opinię/)).toBeInTheDocument()
      })
    })
  })

  describe('SimilarNeedsList', () => {
    it('renders list of similar needs with translated areas', () => {
      const items = [
        { id: '1', excerpt: 'Brak autobusu dla seniora', area: 'AGING' },
        { id: '2', excerpt: 'Trudności w urzędzie', area: 'DIGITAL_EXCLUSION' },
      ]
      renderWithProviders(<SimilarNeedsList items={items} />)

      expect(screen.getByText(/Podobne wyzwania zgłoszone w Małopolsce/)).toBeInTheDocument()
      expect(screen.getByText(/Brak autobusu dla seniora/)).toBeInTheDocument()
      expect(screen.getByText('Wsparcie seniorów i osób starszych')).toBeInTheDocument()
      expect(screen.getByText('Pomoc w korzystaniu z internetu i technologii')).toBeInTheDocument()
    })
  })

  describe('MatchmakingView Integration', () => {
    it('fetches and displays results, focusing heading', async () => {
      const mockInno = new InnovationSummaryJs(
        'inno-1',
        'Innowacja A',
        'Streszczenie A',
        ['AGING'],
        ['SENIORS'],
        'IMPLEMENTED',
      )
      const mockMatch = new MatchJs(mockInno, 0.9, ['Bardzo pasuje'], ['senior'])
      const mockResultJs = new MatchResultJs('need-456', [mockMatch], [], false)

      vi.spyOn(hubApi.matches, 'match').mockResolvedValue(
        new ApiResult<MatchResultJs>(mockResultJs, null),
      )

      renderWithProviders(<MatchmakingView />)

      const sampleBtn = screen.getByRole('button', { name: 'Transport seniorów' })
      fireEvent.click(sampleBtn)

      const submitBtn = screen.getByRole('button', { name: /Wyszukaj sprawdzone rozwiązania/ })
      fireEvent.click(submitBtn)

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 2, name: /Znalezione rozwiązania/ })).toBeInTheDocument()
      })
      expect(screen.getByText('Innowacja A')).toBeInTheDocument()
    })

    it('displays friendly empty state when noGoodMatch is true', async () => {
      const mockResultEmpty = new MatchResultJs('need-empty', [], [], true)

      vi.spyOn(hubApi.matches, 'match').mockResolvedValue(
        new ApiResult<MatchResultJs>(mockResultEmpty, null),
      )

      renderWithProviders(<MatchmakingView />)

      const sampleBtn = screen.getByRole('button', { name: 'Dostępność urzędu' })
      fireEvent.click(sampleBtn)

      const submitBtn = screen.getByRole('button', { name: /Wyszukaj sprawdzone rozwiązania/ })
      fireEvent.click(submitBtn)

      await waitFor(() => {
        expect(
          screen.getByText('Nie znaleźliśmy jeszcze bezpośrednio pasującego rozwiązania'),
        ).toBeInTheDocument()
      })
      expect(
        screen.getByRole('link', { name: 'Zgłoś własny pomysł na innowację' }),
      ).toBeInTheDocument()
    })

    it('handles 429 rate limit error with Polish message', async () => {
      const error429 = new ApiErrorJs(429, 'TOO_MANY_REQUESTS', 'Limit exceeded')
      vi.spyOn(hubApi.matches, 'match').mockResolvedValue(
        new ApiResult<MatchResultJs>(null, error429),
      )

      renderWithProviders(<MatchmakingView />)

      const sampleBtn = screen.getByRole('button', { name: 'Dostępność urzędu' })
      fireEvent.click(sampleBtn)

      const submitBtn = screen.getByRole('button', { name: /Wyszukaj sprawdzone rozwiązania/ })
      fireEvent.click(submitBtn)

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
      })
      expect(screen.getByText(/Zbyt wiele zapytań w krótkim czasie/)).toBeInTheDocument()
    })
  })
})
