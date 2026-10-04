import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { setupServer } from 'msw/node'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { axe } from 'vitest-axe'
import App from '../../App'
import { handlers } from '../../mocks/handlers'
import { resetApplicationMocks } from '../../mocks/applications'

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

vi.mock('../../auth/keycloak', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../auth/keycloak')>()),
  ensureFreshToken: async () => true,
}))

const server = setupServer(...handlers)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  server.resetHandlers()
  cleanup()
  window.localStorage.clear()
  window.sessionStorage.clear()
})
afterAll(() => server.close())
beforeEach(() => resetApplicationMocks())

function LocationProbe() {
  const { pathname, search } = useLocation()
  return <output data-testid="location">{pathname + search}</output>
}

const currentLocation = () => screen.getByTestId('location').textContent ?? ''

function renderApp(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <MemoryRouter initialEntries={[path]}>
          <LocationProbe />
          <App />
        </MemoryRouter>
      </MantineProvider>
    </QueryClientProvider>,
  )
}

const change = (element: HTMLElement, value: string) => fireEvent.change(element, { target: { value } })
// The wizard form itself is labelled by the step heading, so a label query can match it besides the input.
const field = (label: RegExp) => {
  const [input] = screen.getAllByLabelText(label).filter((el) => el.tagName !== 'FORM')
  return input
}
const fill = (label: RegExp, value: string) => change(field(label), value)
const stepHeading = (name: string) => screen.findByRole('heading', { level: 2, name })
const next = (user: ReturnType<typeof userEvent.setup>) => user.click(screen.getByRole('button', { name: /^Dalej/ }))

async function goToStep(user: ReturnType<typeof userEvent.setup>, n: number, title: string) {
  await user.click(screen.getByRole('button', { name: new RegExp(`^${n}\\. ${title}`) }))
  await stepHeading(title)
}

describe('idea card (FE-05)', () => {
  it('rejects an empty step with an error summary and field messages, then submits the card by keyboard-friendly steps', async () => {
    const user = userEvent.setup()
    renderApp('/pomysly/nowy')
    await stepHeading('Na czym polega pomysł')

    await next(user)
    const summary = await screen.findByTestId('error-summary')
    expect(within(summary).getByText(/Tytuł pomysłu: Wpisz tytuł pomysłu/)).toBeInTheDocument()
    expect(field(/^Tytuł pomysłu/)).toHaveAttribute('aria-invalid', 'true')
    expect(field(/^Tytuł pomysłu/)).toHaveAccessibleDescription(/Wpisz tytuł pomysłu/)

    fill(/^Tytuł pomysłu/, 'Kawiarenka internetowa')
    fill(/^Istota pomysłu/, 'Seniorzy uczą się telefonu z młodzieżą.')
    await next(user)
    await stepHeading('Dla kogo jest pomysł')
    await user.click(screen.getByRole('checkbox', { name: 'Seniorzy i osoby starsze' }))
    await next(user)
    await user.click(await screen.findByRole('radio', { name: /Nowy pomysł/ }))
    await next(user)
    await stepHeading('Sprawdź i zgłoś')
    expect(screen.getByText('Kawiarenka internetowa')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Zgłoś pomysł' }))
    expect(await screen.findByText('Pomysł został zgłoszony')).toBeInTheDocument()
    expect(currentLocation()).toMatch(/^\/pomysly\/idea-/)
    expect(window.localStorage.getItem('hubmi.ideaDraft')).toBeNull()
  })

  it('lets the idea card be filled in and sent with the keyboard only', async () => {
    const user = userEvent.setup()
    renderApp('/pomysly/nowy')
    await stepHeading('Na czym polega pomysł')
    act(() => field(/^Tytuł pomysłu/).focus())
    await user.keyboard('Pomysł z klawiatury{Tab}Opis z klawiatury.')
    // Tab on until the Next button (canvas help links may come first); Enter then moves to the next step.
    const nextButton = screen.getByRole('button', { name: /^Dalej/ })
    for (let i = 0; i < 6 && document.activeElement !== nextButton; i++) await user.tab()
    expect(nextButton).toHaveFocus()
    await user.keyboard('{Enter}')
    await stepHeading('Dla kogo jest pomysł')
  })

  it('keeps an unfinished card in the browser and offers to start over', async () => {
    const user = userEvent.setup()
    const first = renderApp('/pomysly/nowy')
    await stepHeading('Na czym polega pomysł')
    fill(/^Tytuł pomysłu/, 'Niedokończony pomysł')
    await waitFor(() => expect(window.localStorage.getItem('hubmi.ideaDraft')).toContain('Niedokończony pomysł'))
    first.unmount()

    renderApp('/pomysly/nowy')
    expect(await screen.findByText('Przywrócono niedokończony pomysł')).toBeInTheDocument()
    expect(field(/^Tytuł pomysłu/)).toHaveValue('Niedokończony pomysł')
    await user.click(screen.getByRole('button', { name: 'Zacznij od nowa' }))
    expect(field(/^Tytuł pomysłu/)).toHaveValue('')
    expect(window.localStorage.getItem('hubmi.ideaDraft')).toBeNull()
  })

  it('lists my ideas with status as text and flags a change since the last visit', async () => {
    window.localStorage.setItem('hubmi.ideaSeen', JSON.stringify({ 'idea-001': 'SUBMITTED|' }))
    renderApp('/pomysly')
    expect(await screen.findByText('Sąsiedzka kawiarenka internetowa dla seniorów')).toBeInTheDocument()
    expect(screen.getByText('W ocenie ROPS')).toBeInTheDocument()
    expect(screen.getByText('Przyjęty')).toBeInTheDocument()
    expect(screen.getAllByText('Zmiana od ostatniej wizyty')).toHaveLength(1)
    expect(screen.getByText(/Komentarz ROPS:/)).toBeInTheDocument()
  })
})

describe('calls (FE-05)', () => {
  it('lists calls by status with dates', async () => {
    renderApp('/nabory')
    expect(await screen.findByRole('heading', { name: 'Trwające nabory' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Nadchodzące nabory' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Zakończone nabory' })).toBeInTheDocument()
    expect(screen.getByText(/Do końca naboru zostało \d+ dni/)).toBeInTheDocument()
  })

  it('does not let anyone start an application in a closed call and says why', async () => {
    renderApp('/nabory/call-closed')
    expect(await screen.findByText(/Wnioski nie są już przyjmowane/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Rozpocznij nowy wniosek/ })).not.toBeInTheDocument()
  })

  it('says when an upcoming call opens', async () => {
    renderApp('/nabory/call-upcoming')
    expect(await screen.findByText(/Nabór jeszcze się nie rozpoczął/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Rozpocznij nowy wniosek/ })).not.toBeInTheDocument()
  })
})

describe('application wizard (FE-05)', () => {
  it('takes an idea all the way to a submitted application', { timeout: 60_000 }, async () => {
    const user = userEvent.setup()
    renderApp('/pomysly/idea-001')
    await user.click(await screen.findByRole('link', { name: /Przygotuj wniosek na podstawie pomysłu/ }))
    expect(currentLocation()).toBe('/nabory?pomysl=idea-001')
    await user.click(await screen.findByRole('link', { name: /edycja jesienna/ }))
    expect(await screen.findByText(/wstępnie wypełniony danymi Twojego pomysłu/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Rozpocznij nowy wniosek' }))

    // 1 Title: prefilled from the idea
    await stepHeading('Tytuł innowacji')
    expect(currentLocation()).toMatch(/^\/wnioski\/app-\d+$/)
    expect(field(/^Tytuł innowacji/)).toHaveValue('Sąsiedzka kawiarenka internetowa dla seniorów')
    expect(screen.getByText('Krok 1 z 8 · pkt 1 wzoru ROPS')).toBeInTheDocument()
    await next(user)

    // 2 Applicant
    await stepHeading('Wnioskodawca')
    await user.click(screen.getByRole('radio', { name: /Osoba fizyczna/ }))
    fill(/^Imię/, 'Jan')
    fill(/^Nazwisko/, 'Testowy')
    fill(/^Ulica/, 'Długa')
    fill(/^Numer budynku/, '5')
    fill(/^Kod pocztowy/, '30-001')
    fill(/^Miejscowość/, 'Kraków')
    fill(/^Telefon/, '123 456 789')
    fill(/^Adres e-mail/, 'jan@example.com')
    await next(user)

    // 3-5 narratives
    await stepHeading('Opis i innowacyjność')
    fill(/^Innowacyjność rozwiązania/, 'Młodzież uczy seniorów.')
    await next(user)
    await stepHeading('Diagnoza problemu')
    change(field(/^Obszar z Mapy Wyzwań Społecznych/), 'AGING')
    await next(user)
    await stepHeading('Odbiorcy i zmiana')
    fill(/^Odbiorcy innowacji/, 'Seniorzy z gminy.')
    fill(/^Zmiana, jaką wprowadza innowacja/, 'Więcej samodzielności.')
    fill(/^Wizja przyszłości/, 'Powielenie w innych gminach.')
    await next(user)

    // 6 Plan and costs
    await stepHeading('Plan działania i koszty')
    const year = String(new Date().getFullYear() + 1)
    await user.click(screen.getByRole('button', { name: /Dodaj pozycję: okres przygotowawczy/ }))
    expect(field(/^Opis zadania/)).toHaveFocus()
    expect(await screen.findByText(/Dodano pozycję 1: Okres przygotowawczy/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Dodaj pozycję: testowanie – faza i$/ }))
    const [prepAction, testAction] = screen.getAllByLabelText(/^Opis zadania/)
    change(prepAction, 'Zakup tabletów')
    change(testAction, 'Zajęcia w świetlicy')
    const [prepMonth, testMonth] = screen.getAllByLabelText(/^Miesiąc/)
    const [prepYear, testYear] = screen.getAllByLabelText(/^Rok/)
    change(prepMonth, '03')
    change(prepYear, year)
    change(testMonth, '05')
    change(testYear, year)
    const [prepCost, testCost] = screen.getAllByLabelText(/^Koszt \(zł\)/)
    change(prepCost, '1000')
    change(testCost, '2000,50')
    expect(screen.getByText(/Okres przygotowawczy: 1 z 3 miesięcy/)).toBeInTheDocument()
    expect(screen.getByText(/Suma kosztów ogółem: 3000,50/)).toBeInTheDocument()
    await next(user)

    // 7 Grant and team
    await stepHeading('Kwota grantu i zespół')
    fill(/^Wnioskowana kwota grantu/, '2500')
    expect(screen.getByText(/Kwota grantu różni się od sumy kosztów o 500,50/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Wpisz sumę kosztów/ }))
    expect(field(/^Wnioskowana kwota grantu/)).toHaveValue('3000,50')
    fill(/^Zespół projektowy/, 'Dwie osoby z doświadczeniem w pracy z seniorami.')
    await next(user)

    // 8 Summary, declarations, submit
    await stepHeading('Oświadczenia i złożenie')
    expect(screen.getByText('Suma kosztów ogółem: 3000,50', { exact: false })).toBeInTheDocument()
    const boxes = await screen.findAllByRole('checkbox')
    expect(boxes).toHaveLength(6)
    for (const box of boxes) await user.click(box)
    await user.click(screen.getByRole('button', { name: 'Złóż wniosek' }))
    const dialog = await screen.findByRole('dialog', { name: 'Złożyć wniosek?' })
    await user.click(within(dialog).getByRole('button', { name: 'Tak, złóż wniosek' }))
    expect(await screen.findByText('Wniosek został złożony', undefined, { timeout: 5000 })).toBeInTheDocument()
    expect(screen.getByText(/Numer wniosku:/)).toBeInTheDocument()

    // Personal data never reach browser storage or the address bar.
    const stored = JSON.stringify({ ...window.localStorage }) + JSON.stringify({ ...window.sessionStorage })
    for (const secret of ['Testowy', 'jan@example.com', '123 456 789', 'Długa']) {
      expect(stored).not.toContain(secret)
      expect(currentLocation()).not.toContain(secret)
    }

    // The submitted application is read-only and shows up in "my applications".
    expect(screen.queryByRole('button', { name: 'Złóż wniosek' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Przejdź do moich wniosków' }))
    expect(await screen.findByText('Złożony')).toBeInTheDocument()
  })

  it('summarises problems after a failed submit and the links lead to the right step and field', async () => {
    const user = userEvent.setup()
    renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')

    await goToStep(user, 8, 'Oświadczenia i złożenie')
    expect(screen.getByText(/Najpierw wybierz, kto składa wniosek/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Złóż wniosek' }))
    const summary = await screen.findByTestId('error-summary')
    await waitFor(() => expect(summary).toHaveFocus())
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await user.click(within(summary).getByRole('link', { name: /Tytuł innowacji: Podaj tytuł innowacji/ }))
    await stepHeading('Tytuł innowacji')
    await waitFor(() => expect(field(/^Tytuł innowacji/)).toHaveFocus())
    expect(field(/^Tytuł innowacji/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('button', { name: /^1\. Tytuł innowacji \(do poprawy: 1\)/ })).toBeInTheDocument()
  })

  it('warns before the applicant variant is changed and drops the typed data on confirmation', async () => {
    const user = userEvent.setup()
    renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')
    await goToStep(user, 2, 'Wnioskodawca')
    await user.click(screen.getByRole('radio', { name: /Osoba fizyczna/ }))
    fill(/^Imię/, 'Jan')

    await user.click(screen.getByRole('radio', { name: /Podmiot/ }))
    const dialog = await screen.findByRole('dialog', { name: 'Zmienić rodzaj wnioskodawcy?' })
    await user.click(within(dialog).getByRole('button', { name: 'Zostaw bez zmian' }))
    expect(screen.getByRole('radio', { name: /Osoba fizyczna/ })).toBeChecked()
    expect(field(/^Imię/)).toHaveValue('Jan')

    await user.click(screen.getByRole('radio', { name: /Podmiot/ }))
    await user.click(await screen.findByRole('button', { name: 'Zmień i usuń dane' }))
    expect(await screen.findByLabelText(/^Numer KRS/)).toHaveValue('')
  })

  it('adds and removes group partners with announcements and a limit of five', async () => {
    const user = userEvent.setup()
    renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')
    await goToStep(user, 2, 'Wnioskodawca')
    await user.click(screen.getByRole('radio', { name: /Grupa nieformalna/ }))
    expect(screen.getByRole('button', { name: /Usuń partnera 1/ })).toBeDisabled()
    for (let i = 0; i < 4; i++) await user.click(screen.getByRole('button', { name: /Dodaj partnera: osobę/ }))
    expect(screen.getByText(/Osiągnięto limit 5 partnerów/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Dodaj partnera/ })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Usuń partnera 5/ }))
    expect(await screen.findByText(/Usunięto partnera 5/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Dodaj partnera: podmiot/ })).toBeInTheDocument()
  })

  it('keeps the draft on the server, not in the browser, and survives a reload', async () => {
    const user = userEvent.setup()
    const first = renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')
    const path = currentLocation().split('?')[0]
    fill(/^Tytuł innowacji/, 'Mój roboczy tytuł')
    await user.click(screen.getByRole('button', { name: /Zapisz szkic/ }))
    expect((await screen.findAllByText(/^Zapisano o \d{2}:\d{2}$/)).length).toBeGreaterThan(0)
    expect(JSON.stringify({ ...window.localStorage })).not.toContain('roboczy')
    first.unmount()

    renderApp(path)
    await stepHeading('Tytuł innowacji')
    expect(field(/^Tytuł innowacji/)).toHaveValue('Mój roboczy tytuł')
  })

  it('saves what was typed when the user leaves the page before the autosave fires', async () => {
    const user = userEvent.setup()
    renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')
    const path = currentLocation().split('?')[0]
    fill(/^Tytuł innowacji/, 'Zapisane przy wyjściu')
    // Straight away, well within the 2.5 s autosave delay.
    await user.click(within(screen.getByRole('navigation', { name: 'Główna nawigacja' })).getByRole('link', { name: 'Moje wnioski' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Moje wnioski' })).toBeInTheDocument()
    await waitFor(async () => {
      const stored = await fetch(`/api/applications/${path.split('/').pop()}`).then((r) => r.json())
      expect(stored.title).toBe('Zapisane przy wyjściu')
    })
  })

  it('marks values that cannot be saved as soon as they are typed and when the server rejects the draft', async () => {
    const user = userEvent.setup()
    renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')
    await goToStep(user, 6, 'Plan działania i koszty')
    await user.click(screen.getByRole('button', { name: /Dodaj pozycję: okres przygotowawczy/ }))

    change(field(/^Koszt \(zł\)/), '12,345')
    expect(await screen.findByText(/Wpisz koszt zadania w złotych/)).toBeInTheDocument()
    expect(screen.getByText(/Część pól ma błędy i nie zostanie zapisana/)).toBeInTheDocument()

    change(field(/^Koszt \(zł\)/), '150000')
    await user.click(screen.getByRole('button', { name: /Zapisz szkic/ }))
    expect(await screen.findByText(/Szkicu nie udało się zapisać/)).toBeInTheDocument()
    expect(field(/^Koszt \(zł\)/)).toHaveAttribute('aria-invalid', 'true')

    change(field(/^Koszt \(zł\)/), '30000000')
    expect(await screen.findByText(/Wpisana kwota jest za duża/)).toBeInTheDocument()
  })

  it('has no accessibility violations on any step', { timeout: 60_000 }, async () => {
    const user = userEvent.setup()
    const { container } = renderApp('/nabory/call-open')
    await user.click(await screen.findByRole('button', { name: 'Rozpocznij nowy wniosek' }))
    await stepHeading('Tytuł innowacji')
    const titles = [
      'Tytuł innowacji',
      'Wnioskodawca',
      'Opis i innowacyjność',
      'Diagnoza problemu',
      'Odbiorcy i zmiana',
      'Plan działania i koszty',
      'Kwota grantu i zespół',
      'Oświadczenia i złożenie',
    ]
    for (const [index, title] of titles.entries()) {
      await goToStep(user, index + 1, title)
      if (title === 'Wnioskodawca') await user.click(screen.getByRole('radio', { name: /Podmiot/ }))
      if (title === 'Plan działania i koszty') {
        await user.click(screen.getByRole('button', { name: /Dodaj pozycję: okres przygotowawczy/ }))
      }
      expect(await axe(container)).toHaveNoViolations()
    }
  })
})
