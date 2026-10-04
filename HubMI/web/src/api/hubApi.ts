import { createHttpHubApi, createMockHubApi, type DemoHubApi, type HubApi } from 'hubmi-client'
import { MOCK_MODE, getAccessToken } from '../auth/keycloak'

/** The demo backend (`VITE_MOCK=true`); it answers from data kept in the browser and lets you choose the account. */
export const demoApi: DemoHubApi | undefined = MOCK_MODE ? createMockHubApi() : undefined

// Generated-from-Kotlin client: same routes and DTOs as the server (module :web-client). In demo mode it is the Kotlin
// mock instead, so the app needs neither the server nor Keycloak.
export const hubApi: HubApi = demoApi ?? createHttpHubApi(window.location.origin, () => getAccessToken())
