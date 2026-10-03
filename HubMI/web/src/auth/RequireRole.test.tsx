import { render, screen, fireEvent } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { RequireRole } from './RequireRole'
import { RequireAuth } from './RequireAuth'
import * as AuthContextModule from './AuthContext'

vi.mock('./AuthContext', () => ({
  useAuth: vi.fn(),
}))

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <MantineProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </MantineProvider>,
  )
}

describe('Route Protection Components', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('RequireAuth', () => {
    it('shows loading state when auth is not ready', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        ready: false,
        authenticated: false,
        username: undefined,
        roles: [],
        hasRole: () => false,
        login: vi.fn(),
        logout: vi.fn(),
      })

      renderWithProviders(
        <RequireAuth>
          <div>Chroniona treść</div>
        </RequireAuth>,
      )

      expect(screen.getByTestId('loading-state')).toBeInTheDocument()
      expect(screen.queryByText('Chroniona treść')).not.toBeInTheDocument()
    })

    it('shows login prompt when user is not authenticated and triggers login', () => {
      const loginMock = vi.fn()
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        ready: true,
        authenticated: false,
        username: undefined,
        roles: [],
        hasRole: () => false,
        login: loginMock,
        logout: vi.fn(),
      })

      renderWithProviders(
        <RequireAuth>
          <div>Chroniona treść</div>
        </RequireAuth>,
      )

      expect(screen.getByTestId('require-auth-prompt')).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: 'Wymagane logowanie' })).toBeInTheDocument()

      const loginBtn = screen.getByRole('button', { name: 'Zaloguj się' })
      fireEvent.click(loginBtn)
      expect(loginMock).toHaveBeenCalledTimes(1)
    })

    it('renders children when authenticated', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        ready: true,
        authenticated: true,
        username: 'jan.kowalski',
        roles: ['user'],
        hasRole: (r) => r === 'user',
        login: vi.fn(),
        logout: vi.fn(),
      })

      renderWithProviders(
        <RequireAuth>
          <div>Chroniona treść</div>
        </RequireAuth>,
      )

      expect(screen.getByText('Chroniona treść')).toBeInTheDocument()
    })
  })

  describe('RequireRole', () => {
    it('renders accessible 403 Forbidden page when user lacks required role', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        ready: true,
        authenticated: true,
        username: 'jan.kowalski',
        roles: ['user'],
        hasRole: (r) => r === 'user',
        login: vi.fn(),
        logout: vi.fn(),
      })

      renderWithProviders(
        <RequireRole requiredRole="admin">
          <div>Tylko dla admina</div>
        </RequireRole>,
      )

      expect(screen.getByTestId('forbidden-page')).toBeInTheDocument()
      expect(screen.getByRole('heading', { level: 1, name: 'Brak uprawnień (403)' })).toBeInTheDocument()
      expect(screen.queryByText('Tylko dla admina')).not.toBeInTheDocument()
    })

    it('renders children when user has required role', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        ready: true,
        authenticated: true,
        username: 'admin.kowalski',
        roles: ['user', 'admin'],
        hasRole: (r) => ['user', 'admin'].includes(r),
        login: vi.fn(),
        logout: vi.fn(),
      })

      renderWithProviders(
        <RequireRole requiredRole="admin">
          <div>Tylko dla admina</div>
        </RequireRole>,
      )

      expect(screen.getByText('Tylko dla admina')).toBeInTheDocument()
      expect(screen.queryByTestId('forbidden-page')).not.toBeInTheDocument()
    })
  })
})
