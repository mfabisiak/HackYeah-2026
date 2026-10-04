import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiErrorJs,
  ApiResult,
  AssistResponseJs,
  FlowJs,
  FlowStepJs,
  InnovationSummaryJs,
  SimilarInnovationJs,
  SimilarPartJs,
  SuggestionJs,
  type AssistListenerJs,
} from 'hubmi-client'
import { hubApi } from '../../api/hubApi'
import { fakeStream } from '../../test/streams'
import { AssistantPage } from './AssistantPage'
import { AssistantPanel } from './AssistantPanel'

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

const idea = { title: 'Dowóz seniorów', essence: 'Wolontariusze wożą seniorów do lekarza.', targetGroups: ['SENIORS'], stage: 'IDEA' }

const transport = new SimilarInnovationJs(
  new InnovationSummaryJs('inno-1', 'Transport door-to-door', 'Dowóz osób z ograniczoną mobilnością.', ['SERVICE_ACCESS'], ['SENIORS'], 'IMPLEMENTED'),
  0.8,
)

function renderPanel(resolveIdea = () => idea) {
  return render(
    <MantineProvider>
      <MemoryRouter>
        <AssistantPanel resolveIdea={resolveIdea} />
      </MemoryRouter>
    </MantineProvider>,
  )
}

/** Starts a call and hands back what the test needs to play the server's part. */
function startCall() {
  const fake = fakeStream<AssistResponseJs>()
  const captured: { listener: AssistListenerJs | null } = { listener: null }
  const spy = vi.spyOn(hubApi.assistant, 'assistDraft').mockImplementation((_idea, _mode, listener) => {
    captured.listener = listener ?? null
    return fake.stream
  })
  return { ...fake, captured, spy }
}

describe('AssistantPanel', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the similar innovations at once and each suggestion as it arrives', async () => {
    const call = startCall()
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Podpowiedz, jak rozwinąć pomysł/ }))

    expect(call.spy).toHaveBeenCalledWith(expect.objectContaining({ title: 'Dowóz seniorów' }), 'EXPAND', expect.anything())
    expect(screen.getByTestId('assistant-working')).toBeInTheDocument()

    await act(async () => call.captured.listener?.onSimilar?.(new SimilarPartJs([transport], 'ALREADY_EXISTS')))
    expect(screen.getByRole('link', { name: 'Transport door-to-door' })).toHaveAttribute('href', '/innowacje/inno-1')
    expect(screen.getByText('Bardzo podobne')).toBeInTheDocument()
    expect(screen.getByTestId('novelty-notice')).toHaveTextContent('Takie rozwiązanie już działa')

    await act(async () =>
      call.captured.listener?.onSuggestion?.(new SuggestionJs('Linia telefoniczna', 'Prosto zamówić przejazd.', 'Uruchom numer.', ['inno-1'])),
    )
    expect(screen.getByText('Linia telefoniczna')).toBeInTheDocument()
    expect(screen.getByTestId('ai-notice')).toHaveTextContent('Wygenerowane przez AI')
    // once as a similar innovation and once as the source of the suggestion
    expect(screen.getAllByRole('link', { name: 'Transport door-to-door' })).toHaveLength(2)

    await act(async () => {
      call.finish(
        new ApiResult(
          new AssistResponseJs(
            'EXPAND',
            'OK',
            true,
            [transport],
            'ALREADY_EXISTS',
            [new SuggestionJs('Linia telefoniczna', 'Prosto zamówić przejazd.', 'Uruchom numer.', ['inno-1'])],
            null,
          ),
          null,
        ),
      )
    })
    await waitFor(() => expect(screen.queryByTestId('assistant-working')).not.toBeInTheDocument())
    expect(screen.getAllByText('Linia telefoniczna')).toHaveLength(1)
  })

  it('shows the flow as a numbered list with the actors', async () => {
    const call = startCall()
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Pokaż, jak to ma działać/ }))
    const flow = new FlowJs(['senior', 'wolontariusz'], [new FlowStepJs('senior', 'wolontariusz', 'zamawia przejazd')])
    await act(async () => call.captured.listener?.onFlow?.(flow))

    expect(screen.getByRole('heading', { name: 'Jak to ma działać' })).toBeInTheDocument()
    expect(screen.getByRole('listitem')).toHaveTextContent('zamawia przejazd')
    expect(screen.getByTestId('ai-notice')).toBeInTheDocument()
  })

  it('says that the model did not answer but keeps what the library knows', async () => {
    const call = startCall()
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Pokaż, co może pójść nie tak/ }))
    await act(async () => {
      call.finish(new ApiResult(new AssistResponseJs('RISKS', 'UNAVAILABLE', true, [transport], 'PARTIAL', [], null), null))
    })

    expect(await screen.findByText(/chwilowo niedostępny/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Transport door-to-door' })).toBeInTheDocument()
    expect(screen.queryByTestId('ai-notice')).not.toBeInTheDocument()
  })

  it('shows an error when the call fails and keeps similar ones that arrived before it', async () => {
    const call = startCall()
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Sprawdź, czy to już istnieje/ }))
    await act(async () => call.captured.listener?.onSimilar?.(new SimilarPartJs([transport], 'PARTIAL')))
    await act(async () => {
      call.finish(new ApiResult<AssistResponseJs>(null, new ApiErrorJs(0, 'NETWORK_ERROR', 'fetch failed', [])))
    })

    expect(await screen.findByTestId('error-alert')).toHaveTextContent('Nie udało się połączyć z serwerem')
    expect(screen.getByRole('link', { name: 'Transport door-to-door' })).toBeInTheDocument()
  })

  it('stops on request, keeps what arrived and cancels the call', async () => {
    const call = startCall()
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Podpowiedz, jak rozwinąć pomysł/ }))
    await act(async () => call.captured.listener?.onSuggestion?.(new SuggestionJs('Pierwsza', 'Bo tak.', 'Zrób to.', [])))
    fireEvent.click(screen.getByRole('button', { name: 'Przerwij' }))

    expect(call.cancel).toHaveBeenCalled()
    expect(screen.getByText(/Przerwano/)).toBeInTheDocument()
    expect(screen.getByText('Pierwsza')).toBeInTheDocument()
    expect(screen.queryByTestId('assistant-working')).not.toBeInTheDocument()
  })

  it('does not call the assistant while the idea is not ready', () => {
    const call = startCall()
    renderPanel(() => null as unknown as typeof idea)

    fireEvent.click(screen.getByRole('button', { name: /Podpowiedz, jak rozwinąć pomysł/ }))

    expect(call.spy).not.toHaveBeenCalled()
  })

  it('cancels the running call when the user leaves the page', () => {
    const call = startCall()
    const { unmount } = renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Podpowiedz, jak rozwinąć pomysł/ }))
    unmount()

    expect(call.cancel).toHaveBeenCalled()
  })
})

describe('AssistantPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('lists what is missing in an error summary instead of calling the assistant', () => {
    const call = startCall()
    render(
      <MantineProvider>
        <MemoryRouter>
          <AssistantPage />
        </MemoryRouter>
      </MantineProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: /Podpowiedz, jak rozwinąć pomysł/ }))

    const summary = screen.getByTestId('error-summary')
    expect(summary).toHaveTextContent('Wpisz tytuł pomysłu.')
    expect(summary).toHaveTextContent('Wybierz przynajmniej jedną grupę odbiorców.')
    expect(call.spy).not.toHaveBeenCalled()
  })

  it('sends the idea as typed once the form is complete', () => {
    const call = startCall()
    render(
      <MantineProvider>
        <MemoryRouter>
          <AssistantPage />
        </MemoryRouter>
      </MantineProvider>,
    )

    fireEvent.change(screen.getByLabelText(/Tytuł pomysłu/), { target: { value: 'Klub dla nastolatków' } })
    fireEvent.change(screen.getByLabelText(/Na czym polega pomysł/), { target: { value: 'Wieczory z psychologiem.' } })
    fireEvent.click(screen.getByLabelText('Dzieci i młodzież'))
    fireEvent.click(screen.getByRole('button', { name: /Sprawdź, czy to już istnieje/ }))

    expect(call.spy).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Klub dla nastolatków', essence: 'Wieczory z psychologiem.', targetGroups: ['YOUTH'], stage: 'IDEA' }),
      'SIMILAR',
      expect.anything(),
    )
  })
})
