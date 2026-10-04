import Keycloak from 'keycloak-js'

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'hubmi',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'hubmi-app',
})

export function clearAuthSession() {
  try {
    keycloak.clearToken()
    // Thoroughly remove any auth/token keys from localStorage and sessionStorage
    // to prevent retry loops when tokens expire or server keysets change
    const keysToRemove: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (
        key &&
        (key.toLowerCase().includes('token') ||
          key.toLowerCase().includes('kc') ||
          key.toLowerCase().includes('auth') ||
          key.toLowerCase().includes('oidc'))
      ) {
        keysToRemove.push(key)
      }
    }
    keysToRemove.forEach((k) => localStorage.removeItem(k))
    sessionStorage.clear()
  } catch {
    // Ignore
  }
}

// The access token lives only in memory (never in localStorage); keep it fresh before it expires.
keycloak.onTokenExpired = () => {
  keycloak.updateToken(30).catch(() => clearAuthSession())
}

// Keycloak can be initialised only once; React StrictMode runs effects twice in development.
let initialisation: Promise<boolean> | undefined

export function initKeycloak(): Promise<boolean> {
  initialisation ??= keycloak
    .init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    })
    .catch((err) => {
      console.warn('Keycloak initialization failed, clearing any stale session:', err)
      clearAuthSession()
      return false
    })
  return initialisation
}

export const getAccessToken = (): string | undefined => keycloak.token

/**
 * Demo mode (`VITE_MOCK=true`) runs without server and Keycloak: `hubApi` is the Kotlin mock and the user is a fixed
 * demo user, signed in and out by the buttons only.
 */
export const MOCK_MODE = import.meta.env.VITE_MOCK === 'true'

/**
 * Renews the access token when it is about to expire. `false` means no usable token is left (the session ended or the
 * refresh failed while the old token has already expired); a failed refresh with a still valid token is not an error.
 */
export async function ensureFreshToken(minValiditySeconds = 30): Promise<boolean> {
  if (MOCK_MODE) return true
  if (!keycloak.authenticated) return false
  return keycloak.updateToken(minValiditySeconds).then(
    () => true,
    () => !keycloak.isTokenExpired(),
  )
}
