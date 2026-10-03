import Keycloak from 'keycloak-js'

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'hubmi',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'hubmi-app',
})

// The access token lives only in memory (never in localStorage); keep it fresh before it expires.
keycloak.onTokenExpired = () => {
  keycloak.updateToken(30).catch(() => keycloak.clearToken())
}

// Keycloak can be initialised only once; React StrictMode runs effects twice in development.
let initialisation: Promise<boolean> | undefined

export function initKeycloak(): Promise<boolean> {
  initialisation ??= keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
  })
  return initialisation
}

export const getAccessToken = (): string | undefined => keycloak.token
