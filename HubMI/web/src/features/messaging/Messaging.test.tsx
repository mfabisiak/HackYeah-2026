import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { NotificationBell } from './NotificationBell'
import { NotificationsDrawer } from './NotificationsDrawer'
import { NotificationsProvider } from './useNotifications'
import { ThreadsListPage } from './ThreadsListPage'
import { ThreadDetailPage } from './ThreadDetailPage'
import { NewThreadModal } from './NewThreadModal'
import { hubApi } from '../../api/hubApi'
import {
  ApiErrorJs,
  ApiResult,
  EmptyJs,
  MessageJs,
  NotificationJs,
  PageJs,
  ThreadJs,
} from 'hubmi-client'

let mockAuth = {
  ready: true,
  authenticated: true,
  username: 'jan.senior',
  roles: ['user'],
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
      <NotificationsProvider>
        <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
      </NotificationsProvider>
    </MantineProvider>,
  )
}

const mockNotifications: NotificationJs[] = [
  new NotificationJs(
    'notif-1',
    'MESSAGE_RECEIVED',
    'Nowa wiadomość od pracownika ROPS',
    'Odpowiedzieliśmy na Twoje pytanie w sprawie wolontariatu sąsiedzkiego.',
    '2026-10-04T10:30:00Z',
    false,
  ),
  new NotificationJs(
    'notif-2',
    'IDEA_STATUS_CHANGED',
    'Twój pomysł został zaakceptowany',
    'Gratulacje! Pomysł przeszedł wstępną weryfikację formalną.',
    '2026-10-03T14:15:00Z',
    true,
  ),
]

const mockThreads: ThreadJs[] = [
  new ThreadJs(
    'thread-001',
    'Wsparcie dla klubu seniora w gminie Niepołomice',
    'idea-123',
    '2026-10-04T11:00:00Z',
    true,
  ),
  new ThreadJs(
    'thread-002',
    'Pytanie o procedurę testowania innowacji',
    null,
    '2026-10-02T09:00:00Z',
    false,
  ),
]

const mockMessages: MessageJs[] = [
  new MessageJs(
    'msg-1',
    'Jan Senior',
    'AUTHOR',
    'Dzień dobry, chciałbym dowiedzieć się, kiedy rusza nabór na testowanie innowacji w naszym powiecie?',
    '2026-10-04T10:00:00Z',
  ),
  new MessageJs(
    'msg-2',
    'Anna Koordynator ROPS',
    'ADMIN',
    'Dzień dobry Panie Janie! Nabór startuje w przyszłym tygodniu. Z przyjemnością pomożemy przygotować zgłoszenie.',
    '2026-10-04T10:30:00Z',
  ),
  new MessageJs(
    'msg-3',
    'Piotr Ekspert',
    'EXPERT',
    'Dodatkowo przygotowaliśmy bezpłatne materiały edukacyjne w bazie wiedzy.',
    '2026-10-04T11:00:00Z',
  ),
]

describe('Komunikacja i powiadomienia (FE-07)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    mockAuth = {
      ready: true,
      authenticated: true,
      username: 'jan.senior',
      roles: ['user'],
      hasRole: (role: string) => mockAuth.roles.includes(role),
      login: vi.fn(),
      logout: vi.fn(),
    }
  })

  describe('Powiadomienia i dzwonek w nagłówku', () => {
    it('wyświetla licznik nieprzeczytanych powiadomień i etykietę dla czytnika ekranu', async () => {
      vi.spyOn(hubApi.notifications, 'list').mockResolvedValue(
        new ApiResult(
          { items: mockNotifications, total: 2, page: 0, size: 50 } as PageJs<NotificationJs>,
          null,
        ),
      )

      renderWithProviders(<NotificationBell />)

      await waitFor(() => {
        expect(
          screen.getByRole('button', { name: /Powiadomienia: 1 nieprzeczytanych/i }),
        ).toBeInTheDocument()
      })

      // Badge displays "1"
      expect(screen.getByText('1')).toBeInTheDocument()
    })

    it('otwiera panel powiadomień, pozwala oznaczyć jako przeczytane i filtrować', async () => {
      vi.spyOn(hubApi.notifications, 'list').mockResolvedValue(
        new ApiResult(
          { items: mockNotifications, total: 2, page: 0, size: 50 } as PageJs<NotificationJs>,
          null,
        ),
      )
      const markReadSpy = vi.spyOn(hubApi.notifications, 'markRead').mockResolvedValue(
        new ApiResult(new EmptyJs(), null),
      )

      renderWithProviders(<NotificationsDrawer opened={true} onClose={vi.fn()} />)

      await waitFor(() => {
        expect(screen.getByText('Nowa wiadomość od pracownika ROPS')).toBeInTheDocument()
        expect(screen.getByText('Twój pomysł został zaakceptowany')).toBeInTheDocument()
      })

      // Click "Oznacz jako przeczytane"
      const markButton = screen.getByRole('button', { name: /Oznacz jako przeczytane/i })
      fireEvent.click(markButton)

      await waitFor(() => {
        expect(markReadSpy).toHaveBeenCalledWith('notif-1')
      })
    })
  })

  describe('Lista rozmów z ROPS (ThreadsListPage)', () => {
    it('wyświetla listę wątków, statusy nieprzeczytanych i daty', async () => {
      vi.spyOn(hubApi.threads, 'list').mockResolvedValue(
        new ApiResult(
          { items: mockThreads, total: 2, page: 0, size: 50 } as PageJs<ThreadJs>,
          null,
        ),
      )

      renderWithProviders(<ThreadsListPage />)

      expect(screen.getByRole('heading', { level: 1, name: 'Wiadomości i kontakt z ROPS' })).toBeInTheDocument()
      expect(screen.getByText('Jak działa skrzynka wiadomości?')).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.getByText('Wsparcie dla klubu seniora w gminie Niepołomice')).toBeInTheDocument()
        expect(screen.getByText('Pytanie o procedurę testowania innowacji')).toBeInTheDocument()
      })

      // Badges
      expect(screen.getByText('Nowa odpowiedź / Nieprzeczytana')).toBeInTheDocument()
      expect(screen.getByText('Przeczytane')).toBeInTheDocument()
      expect(screen.getByText('Dotyczy pomysłu')).toBeInTheDocument()
    })

    it('pozwala administratorowi filtrować wątki wymagające odpowiedzi', async () => {
      mockAuth.roles = ['admin']

      vi.spyOn(hubApi.threads, 'list').mockResolvedValue(
        new ApiResult(
          { items: mockThreads, total: 2, page: 0, size: 50 } as PageJs<ThreadJs>,
          null,
        ),
      )

      renderWithProviders(<ThreadsListPage />)

      await waitFor(() => {
        expect(screen.getByText('Wsparcie dla klubu seniora w gminie Niepołomice')).toBeInTheDocument()
      })

      // Admin segmented control is visible
      const unreadFilter = screen.getByText('Wymaga odpowiedzi (nowe)')
      fireEvent.click(unreadFilter)

      // Only unread thread should remain visible
      expect(screen.getByText('Wsparcie dla klubu seniora w gminie Niepołomice')).toBeInTheDocument()
      expect(screen.queryByText('Pytanie o procedurę testowania innowacji')).not.toBeInTheDocument()
    })
  })

  describe('Tworzenie nowej wiadomości (NewThreadModal)', () => {
    it('waliduje formularz i wysyła nową wiadomość do ROPS', async () => {
      const createSpy = vi.spyOn(hubApi.threads, 'create').mockResolvedValue(
        new ApiResult(
          new ThreadJs('new-1', 'Nowy temat', null, '2026-10-04T12:00:00Z', false),
          null,
        ),
      )

      const onCreatedMock = vi.fn()
      const onCloseMock = vi.fn()

      renderWithProviders(
        <NewThreadModal
          opened={true}
          onClose={onCloseMock}
          onThreadCreated={onCreatedMock}
        />,
      )

      expect(screen.getByText('Napisz nową wiadomość do ROPS')).toBeInTheDocument()

      const subjectInput = screen.getByLabelText(/Temat rozmowy/i)
      const messageInput = screen.getByLabelText(/Treść wiadomości/i)
      const submitBtn = screen.getByRole('button', { name: 'Wyślij wiadomość' })

      // Trigger validation with empty inputs
      fireEvent.click(submitBtn)
      expect(screen.getByText('Proszę podać temat rozmowy.')).toBeInTheDocument()

      // Fill valid content
      fireEvent.change(subjectInput, { target: { value: 'Pytanie o dofinansowanie klubu' } })
      fireEvent.change(messageInput, { target: { value: 'Chcemy założyć klub seniora w sołectwie.' } })

      fireEvent.click(submitBtn)

      await waitFor(() => {
        expect(createSpy).toHaveBeenCalledWith(
          'Pytanie o dofinansowanie klubu',
          'Chcemy założyć klub seniora w sołectwie.',
          null,
        )
      })

      expect(onCloseMock).toHaveBeenCalled()
      expect(onCreatedMock).toHaveBeenCalledWith('new-1')
    })
  })

  describe('Szczegóły rozmowy i odpowiedzi (ThreadDetailPage)', () => {
    it('wyświetla historię wiadomości z etykietami ról (ROPS, Autor, Ekspert)', async () => {
      vi.spyOn(hubApi.threads, 'messages').mockResolvedValue(
        new ApiResult(mockMessages, null),
      )
      vi.spyOn(hubApi.threads, 'list').mockResolvedValue(
        new ApiResult(
          { items: mockThreads, total: 2, page: 0, size: 50 } as PageJs<ThreadJs>,
          null,
        ),
      )

      renderWithProviders(
        <Routes>
          <Route path="/wiadomosci/:threadId" element={<ThreadDetailPage />} />
        </Routes>,
        ['/wiadomosci/thread-001'],
      )

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Wsparcie dla klubu seniora w gminie Niepołomice' })).toBeInTheDocument()
      })

      // Semantic log role
      expect(screen.getByRole('log', { name: 'Historia wiadomości w wątku' })).toBeInTheDocument()

      // Sender roles
      expect(screen.getByText('Pracownik ROPS')).toBeInTheDocument()
      expect(screen.getByText('Ty (Autor)')).toBeInTheDocument()
      expect(screen.getByText('Ekspert merytoryczny')).toBeInTheDocument()

      // Message texts
      expect(screen.getByText(/kiedy rusza nabór na testowanie innowacji/i)).toBeInTheDocument()
      expect(screen.getByText(/Dzień dobry Panie Janie! Nabór startuje w przyszłym tygodniu/i)).toBeInTheDocument()
    })

    it('wysyła odpowiedź użytkownika i dodaje ją do logu rozmowy', async () => {
      vi.spyOn(hubApi.threads, 'messages').mockResolvedValue(
        new ApiResult(mockMessages, null),
      )
      vi.spyOn(hubApi.threads, 'list').mockResolvedValue(
        new ApiResult(
          { items: mockThreads, total: 2, page: 0, size: 50 } as PageJs<ThreadJs>,
          null,
        ),
      )
      const postSpy = vi.spyOn(hubApi.threads, 'postMessage').mockResolvedValue(
        new ApiResult(
          new MessageJs(
            'msg-new',
            'Jan Senior',
            'AUTHOR',
            'Bardzo dziękuję za szybką i pomocną odpowiedź!',
            '2026-10-04T12:05:00Z',
          ),
          null,
        ),
      )

      renderWithProviders(
        <Routes>
          <Route path="/wiadomosci/:threadId" element={<ThreadDetailPage />} />
        </Routes>,
        ['/wiadomosci/thread-001'],
      )

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Wsparcie dla klubu seniora w gminie Niepołomice' })).toBeInTheDocument()
      })

      const replyTextarea = screen.getByLabelText(/Treść Twojej odpowiedzi/i)
      fireEvent.change(replyTextarea, {
        target: { value: 'Bardzo dziękuję za szybką i pomocną odpowiedź!' },
      })

      const sendBtn = screen.getByRole('button', { name: 'Wyślij odpowiedź' })
      fireEvent.click(sendBtn)

      await waitFor(() => {
        expect(postSpy).toHaveBeenCalledWith(
          'thread-001',
          'Bardzo dziękuję za szybką i pomocną odpowiedź!',
        )
      })

      expect(screen.getByText('Bardzo dziękuję za szybką i pomocną odpowiedź!')).toBeInTheDocument()
    })

    it('obsługuje błąd 404 czytelnym komunikatem i linkiem powrotu', async () => {
      vi.spyOn(hubApi.threads, 'messages').mockResolvedValue(
        new ApiResult<MessageJs[]>(
          null as unknown as MessageJs[],
          new ApiErrorJs(404, 'NOT_FOUND', 'Nie znaleziono wątku'),
        ),
      )

      renderWithProviders(
        <Routes>
          <Route path="/wiadomosci/:threadId" element={<ThreadDetailPage />} />
        </Routes>,
        ['/wiadomosci/obcy-watek'],
      )

      await waitFor(() => {
        expect(screen.getByText('Rozmowa nie istnieje lub brak dostępu')).toBeInTheDocument()
      })

      expect(screen.getByRole('link', { name: /Wróć do listy wiadomości/i })).toBeInTheDocument()
    })
  })
})
