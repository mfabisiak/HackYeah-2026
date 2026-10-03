import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { initKeycloak, keycloak } from './keycloak'

interface AuthState {
  ready: boolean
  authenticated: boolean
  username: string | undefined
  login: () => void
  logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const [authenticated, setAuthenticated] = useState(false)

  useEffect(() => {
    initKeycloak()
      .then(setAuthenticated)
      .catch(() => setAuthenticated(false))
      .finally(() => setReady(true))
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      ready,
      authenticated,
      username: keycloak.tokenParsed?.preferred_username as string | undefined,
      login: () => void keycloak.login({ redirectUri: window.location.href }),
      logout: () => void keycloak.logout({ redirectUri: window.location.origin }),
    }),
    [ready, authenticated],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
