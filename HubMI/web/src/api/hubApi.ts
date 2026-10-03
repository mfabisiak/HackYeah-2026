import { HubApi } from 'hubmi-client'
import { getAccessToken } from '../auth/keycloak'

// Generated-from-Kotlin client: same routes and DTOs as the server (module :web-client).
export const hubApi = new HubApi(window.location.origin, () => getAccessToken())
