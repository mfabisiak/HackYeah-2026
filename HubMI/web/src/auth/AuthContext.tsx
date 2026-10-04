import { useQueryClient } from '@tanstack/react-query'
import type { DemoAccountJs } from 'hubmi-client'
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { demoApi } from '../api/hubApi'
import { clearIdeaLocalData } from '../features/ideas/localData'
import { MOCK_MODE, clearAuthSession, initKeycloak, keycloak } from './keycloak'

export interface AuthState {
  ready: boolean
  authenticated: boolean
  username: string | undefined
  roles: string[]
  hasRole: (role: string) => boolean
  login: () => void
  logout: () => void
  /** Demo only: the accounts one can sign in as, the signed-in one and the way to switch (`Role` name). */
  demo?: {
    accounts: DemoAccountJs[]
    current: DemoAccountJs
    signInAs: (role: string) => void
  }
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(MOCK_MODE)
  const [authenticated, setAuthenticated] = useState(MOCK_MODE)
  const [account, setAccount] = useState(() => demoApi?.currentAccount())
  const queryClient = useQueryClient()

  useEffect(() => {
    if (MOCK_MODE) return
    initKeycloak()
      .then((signedIn) => {
        // Nobody is signed in: whatever the previous person left on this computer must not be shown to the next one.
        if (!signedIn) {
          clearIdeaLocalData()
        }
        setAuthenticated(signedIn)
      })
      .catch(() => {
        clearAuthSession()
        clearIdeaLocalData()
        setAuthenticated(false)
      })
      .finally(() => setReady(true))

    keycloak.onAuthLogout = () => {
      clearAuthSession()
      clearIdeaLocalData()
      setAuthenticated(false)
    }
    keycloak.onAuthRefreshError = () => {
      clearAuthSession()
      clearIdeaLocalData()
      setAuthenticated(false)
    }
  }, [])

  const roles = useMemo<string[]>(() => {
    if (!authenticated) return []
    if (account) return Array.from(account.roles)
    const realmRoles = (keycloak.tokenParsed?.realm_access?.roles as string[] | undefined) ?? []
    return realmRoles
  }, [authenticated, account])

  const value = useMemo<AuthState>(
    () => ({
      ready,
      authenticated,
      username: account ? account.username : (keycloak.tokenParsed?.preferred_username as string | undefined),
      roles,
      hasRole: (role: string) => roles.includes(role) || (typeof keycloak.hasRealmRole === 'function' ? keycloak.hasRealmRole(role) : false),
      login: () => {
        clearAuthSession()
        if (MOCK_MODE) setAuthenticated(true)
        else void keycloak.login({ redirectUri: window.location.href })
      },
      logout: () => {
        // Nothing the user typed may stay behind on a shared computer.
        clearAuthSession()
        clearIdeaLocalData()
        if (MOCK_MODE) setAuthenticated(false)
        else void keycloak.logout({ redirectUri: window.location.origin })
      },
      demo:
        demoApi && account
          ? {
              accounts: Array.from(demoApi.accounts()),
              current: account,
              signInAs: (role: string) => {
                const next = demoApi?.signInAs(role)
                if (!next) return
                // What was fetched for the previous account must not be shown to the next one.
                queryClient.clear()
                setAccount(next)
                setAuthenticated(true)
              },
            }
          : undefined,
    }),
    [ready, authenticated, roles, account, queryClient],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
