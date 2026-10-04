import { createHttpHubApi, createMockHubApi, type HubApi } from 'hubmi-client'
import { MOCK_MODE, getAccessToken } from '../auth/keycloak'

// Generated-from-Kotlin client: same routes and DTOs as the server (module :web-client). In demo mode (`VITE_MOCK=true`)
// it answers from data kept in the browser instead, so the app needs neither the server nor Keycloak.
export const hubApi: HubApi = MOCK_MODE ? createMockHubApi() : createHttpHubApi(window.location.origin, () => getAccessToken())
