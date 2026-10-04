import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  AdaptationJs,
  AdaptationPlanJs,
  AdaptationResponseJs,
  AdaptationStepJs,
  ApiErrorJs,
  ApiResult,
  InstitutionJs,
  type AdaptationListenerJs,
  type PageJs,
} from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { fakeStream } from '../../test/streams'
import { AdaptationModal } from './AdaptationModal'
import { AdaptationPlanView } from './AdaptationPlanView'
import { AdaptationsReviewQueue } from './AdaptationsReviewQueue'
import { MyAdaptationsPage } from './MyAdaptationsPage'
import { ReviewModal } from './ReviewModal'

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    ready: true,
    authenticated: true,
    username: 'anna.admin',
    roles: ['user', 'admin'],
    hasRole: () => true,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}))

function adaptation(overrides: { status?: string; exceedsBudget?: boolean; comment?: string | null; id?: string } = {}) {
  return new AdaptationJs(
    overrides.id ?? 'ad-1',
    'inno-1',
    'Transport door-to-door',
    new InstitutionJs('NGO', 4, 30_000, 'Małe miasto'),
    new AdaptationPlanJs(
      'Usługa przewozu prowadzona przez stowarzyszenie',
      [new AdaptationStepJs('Rekrutacja kierowców', 'Nabór wolontariuszy.', 1), new AdaptationStepJs('Rozeznanie potrzeb', 'Lista seniorów.', 0)],
      ['Trzy busy', 'Koordynator'],
      ['Za mało kierowców'],
      overrides.exceedsBudget ? 45_000 : 25_000,
      overrides.exceedsBudget ?? false,
    ),
    true,
    'bielik-4.5b',
    overrides.status ?? 'PENDING_REVIEW',
    overrides.comment ?? null,
    '2026-10-04T10:00:00Z',
  )
}

function page(items: AdaptationJs[]): PageJs<AdaptationJs> {
  return { items, total: items.length, page: 0, size: 10 } as PageJs<AdaptationJs>
}

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <MantineProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </MantineProvider>,
  )
}

describe('AdaptationPlanView', () => {
  it('shows the plan with its AI label, steps in order, resources, risks and cost', () => {
    renderWithProviders(<AdaptationPlanView adaptation={adaptation()} />)

    expect(screen.getByTestId('ai-notice')).toHaveTextContent('Czeka na przegląd pracownika ROPS')
    expect(screen.getByText('Czeka na przegląd ROPS')).toBeInTheDocument()
    expect(screen.getByText('Usługa przewozu prowadzona przez stowarzyszenie')).toBeInTheDocument()
    expect(screen.getByText('Miesiąc 2')).toBeInTheDocument()
    expect(screen.getByText('Trzy busy')).toBeInTheDocument()
    expect(screen.getByText('Za mało kierowców')).toBeInTheDocument()
    expect(screen.getByText(/25\s000 zł/)).toBeInTheDocument()
    expect(screen.queryByText('Koszt przekracza budżet')).not.toBeInTheDocument()
  })

  it('warns in words when the cost is above the budget and shows the admin comment', () => {
    renderWithProviders(
      <AdaptationPlanView adaptation={adaptation({ exceedsBudget: true, status: 'REJECTED', comment: 'Za drogo.' })} />,
    )

    expect(screen.getByText('Koszt przekracza budżet')).toBeInTheDocument()
    expect(screen.getByText('Za drogo.')).toBeInTheDocument()
    expect(screen.getByText('Odrzucony przez ROPS')).toBeInTheDocument()
  })
})

describe('AdaptationModal', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  function startCall() {
    const fake = fakeStream<AdaptationResponseJs>()
    const captured: { listener: AdaptationListenerJs | null } = { listener: null }
    const spy = vi.spyOn(hubApi.adaptations, 'request').mockImplementation((_id, _institution, listener) => {
      captured.listener = listener ?? null
      return fake.stream
    })
    return { ...fake, captured, spy }
  }

  function openModal() {
    renderWithProviders(<AdaptationModal opened onClose={vi.fn()} innovationId="inno-1" innovationTitle="Transport door-to-door" />)
  }

  function fillForm() {
    fireEvent.click(screen.getAllByLabelText(/Kim jesteś/)[0])
    fireEvent.click(screen.getByRole('option', { name: 'Organizacja pozarządowa' }))
    fireEvent.change(screen.getByLabelText(/Jakie są warunki u Was/), { target: { value: 'Małe miasto, mamy 3 busy.' } })
  }

  it('lists what is missing instead of calling the Middleman', () => {
    const call = startCall()
    openModal()

    fireEvent.click(screen.getByRole('button', { name: 'Przygotuj plan' }))

    expect(screen.getByTestId('error-summary')).toHaveTextContent('Wybierz rodzaj instytucji.')
    expect(screen.getByTestId('error-summary')).toHaveTextContent('Opisz w kilku zdaniach warunki w Twojej instytucji.')
    expect(call.spy).not.toHaveBeenCalled()
  })

  it('writes the plan live and ends with the stored plan', async () => {
    const call = startCall()
    openModal()
    fillForm()

    fireEvent.click(screen.getByRole('button', { name: 'Przygotuj plan' }))

    expect(call.spy).toHaveBeenCalledWith(
      'inno-1',
      expect.objectContaining({ type: 'NGO', staffCount: 3, budgetPln: 50_000, context: 'Małe miasto, mamy 3 busy.' }),
      expect.anything(),
    )
    expect(screen.getByTestId('adaptation-working')).toBeInTheDocument()

    await act(async () => call.captured.listener?.onStep?.(new AdaptationStepJs('Rozeznanie potrzeb', 'Lista seniorów.', 0)))
    expect(screen.getByText('Rozeznanie potrzeb')).toBeInTheDocument()

    await act(async () => {
      call.finish(new ApiResult(new AdaptationResponseJs('OK', adaptation()), null))
    })

    expect(await screen.findByText('Plan zapisany')).toBeInTheDocument()
    expect(screen.getByTestId('adaptation-plan')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Moje plany adaptacji' })).toHaveAttribute('href', '/moje-plany')
    expect(screen.queryByTestId('adaptation-working')).not.toBeInTheDocument()
  })

  it('says that nothing was stored when the model did not write a plan, and keeps the form', async () => {
    const call = startCall()
    openModal()
    fillForm()

    fireEvent.click(screen.getByRole('button', { name: 'Przygotuj plan' }))
    await act(async () => {
      call.finish(new ApiResult(new AdaptationResponseJs('UNAVAILABLE', null), null))
    })

    expect(await screen.findByText('Plan nie powstał')).toBeInTheDocument()
    expect(screen.getByText(/chwilowo niedostępny/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Jakie są warunki u Was/)).toHaveValue('Małe miasto, mamy 3 busy.')
  })

  it('stops on request, cancels the call and returns to the form', async () => {
    const call = startCall()
    openModal()
    fillForm()

    fireEvent.click(screen.getByRole('button', { name: 'Przygotuj plan' }))
    fireEvent.click(screen.getByRole('button', { name: 'Przerwij' }))

    expect(call.cancel).toHaveBeenCalled()
    expect(await screen.findByText(/nic nie zostało zapisane/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Przygotuj plan' })).toBeInTheDocument()
  })
})

describe('ReviewModal', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('needs a reason to reject and does not call the API without it', async () => {
    const review = vi.spyOn(hubApi.admin, 'reviewAdaptation')
    renderWithProviders(
      <ReviewModal review={{ adaptation: adaptation(), decision: 'REJECTED' }} onClose={vi.fn()} onReviewed={vi.fn()} onConflict={vi.fn()} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Odrzuć plan' }))

    expect(await screen.findByText('Napisz autorowi, dlaczego odrzucasz plan.')).toBeInTheDocument()
    expect(review).not.toHaveBeenCalled()
  })

  it('sends the decision with the comment', async () => {
    const reviewed = adaptation({ status: 'REJECTED', comment: 'Za drogo.' })
    const review = vi.spyOn(hubApi.admin, 'reviewAdaptation').mockResolvedValue(new ApiResult(reviewed, null))
    const onReviewed = vi.fn()
    renderWithProviders(
      <ReviewModal review={{ adaptation: adaptation(), decision: 'REJECTED' }} onClose={vi.fn()} onReviewed={onReviewed} onConflict={vi.fn()} />,
    )

    fireEvent.change(screen.getByLabelText(/Powód odrzucenia/), { target: { value: 'Za drogo.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Odrzuć plan' }))

    await waitFor(() => expect(onReviewed).toHaveBeenCalledWith(reviewed))
    expect(review).toHaveBeenCalledWith('ad-1', 'REJECTED', 'Za drogo.')
  })

  it('reports a conflict when someone else decided first', async () => {
    vi.spyOn(hubApi.admin, 'reviewAdaptation').mockResolvedValue(
      new ApiResult<AdaptationJs>(null, new ApiErrorJs(409, 'CONFLICT', 'Plan ma status APPROVED', [])),
    )
    const onConflict = vi.fn()
    renderWithProviders(
      <ReviewModal review={{ adaptation: adaptation(), decision: 'APPROVED' }} onClose={vi.fn()} onReviewed={vi.fn()} onConflict={onConflict} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Zatwierdź plan' }))

    await waitFor(() => expect(onConflict).toHaveBeenCalled())
  })
})

describe('AdaptationsReviewQueue', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('lists the plans waiting for review and approves one after a confirmation', async () => {
    const list = vi.spyOn(hubApi.admin, 'adaptations').mockResolvedValue(new ApiResult(page([adaptation()]), null))
    const review = vi
      .spyOn(hubApi.admin, 'reviewAdaptation')
      .mockResolvedValue(new ApiResult(adaptation({ status: 'APPROVED' }), null))
    renderWithProviders(<AdaptationsReviewQueue />)

    expect(await screen.findByText('Transport door-to-door')).toBeInTheDocument()
    expect(list).toHaveBeenCalledWith('PENDING_REVIEW', 0, 10)

    fireEvent.click(screen.getByRole('button', { name: /Transport door-to-door/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Zatwierdź' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Zatwierdź plan' }))

    await waitFor(() => expect(review).toHaveBeenCalledWith('ad-1', 'APPROVED', undefined))
    expect(await screen.findByText(/Zapisano decyzję/)).toBeInTheDocument()
    expect(list).toHaveBeenCalledTimes(2)
  })

  it('shows an empty state when nothing waits', async () => {
    vi.spyOn(hubApi.admin, 'adaptations').mockResolvedValue(new ApiResult(page([]), null))
    renderWithProviders(<AdaptationsReviewQueue />)

    expect(await screen.findByText('Brak planów w tym widoku')).toBeInTheDocument()
  })

  it('shows the error with a retry when the list cannot be loaded', async () => {
    vi.spyOn(hubApi.admin, 'adaptations').mockResolvedValue(
      new ApiResult<PageJs<AdaptationJs>>(null, new ApiErrorJs(0, 'NETWORK_ERROR', 'fetch failed', [])),
    )
    renderWithProviders(<AdaptationsReviewQueue />)

    expect(await screen.findByTestId('error-alert')).toHaveTextContent('Nie udało się połączyć z serwerem')
    expect(screen.getByRole('button', { name: 'Spróbuj ponownie' })).toBeInTheDocument()
  })
})

describe('MyAdaptationsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the caller’s plans with their review status', async () => {
    vi.spyOn(hubApi.adaptations, 'mine').mockResolvedValue(
      new ApiResult(page([adaptation({ status: 'APPROVED', comment: 'Do pilotażu.' })]), null),
    )
    renderWithProviders(<MyAdaptationsPage />)

    expect(await screen.findByText('Transport door-to-door')).toBeInTheDocument()
    expect(screen.getAllByText('Zatwierdzony przez ROPS').length).toBeGreaterThan(0)
    expect(screen.getByText('Liczba planów: 1')).toBeInTheDocument()
  })

  it('invites to the library when there is no plan yet', async () => {
    vi.spyOn(hubApi.adaptations, 'mine').mockResolvedValue(new ApiResult(page([]), null))
    renderWithProviders(<MyAdaptationsPage />)

    expect(await screen.findByText('Nie masz jeszcze żadnego planu')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Przejdź do bazy innowacji' })).toHaveAttribute('href', '/innowacje')
  })
})
