import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { clearAuthSession, initKeycloak, keycloak } from './keycloak'

export interface AuthState {
  ready: boolean
  authenticated: boolean
  username: string | undefined
  roles: string[]
  hasRole: (role: string) => boolean
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
      .catch(() => {
        clearAuthSession()
        setAuthenticated(false)
      })
      .finally(() => setReady(true))

    keycloak.onAuthLogout = () => {
      clearAuthSession()
      setAuthenticated(false)
    }
    keycloak.onAuthRefreshError = () => {
      clearAuthSession()
      setAuthenticated(false)
    }
  }, [])

  const roles = useMemo<string[]>(() => {
    if (!authenticated) return []
    const realmRoles = (keycloak.tokenParsed?.realm_access?.roles as string[] | undefined) ?? []
    return realmRoles
  }, [authenticated])

  const value = useMemo<AuthState>(
    () => ({
      ready,
      authenticated,
      username: keycloak.tokenParsed?.preferred_username as string | undefined,
      roles,
      hasRole: (role: string) => roles.includes(role) || (typeof keycloak.hasRealmRole === 'function' ? keycloak.hasRealmRole(role) : false),
      login: () => {
        clearAuthSession()
        void keycloak.login({ redirectUri: window.location.href })
      },
      logout: () => {
        clearAuthSession()
        void keycloak.logout({ redirectUri: window.location.origin })
      },
    }),
    [ready, authenticated, roles],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
