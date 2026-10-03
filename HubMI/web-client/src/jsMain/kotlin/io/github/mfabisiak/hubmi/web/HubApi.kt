package io.github.mfabisiak.hubmi.web

import io.github.mfabisiak.hubmi.api.Api
import io.github.mfabisiak.hubmi.api.Health
import io.github.mfabisiak.hubmi.api.HealthResponse
import io.github.mfabisiak.hubmi.api.MeResponse
import io.github.mfabisiak.hubmi.web.admin.AdminApi
import io.github.mfabisiak.hubmi.web.ideas.ApplicationsApi
import io.github.mfabisiak.hubmi.web.ideas.CallsApi
import io.github.mfabisiak.hubmi.web.ideas.IdeasApi
import io.github.mfabisiak.hubmi.web.innovations.InnovationsApi
import io.github.mfabisiak.hubmi.web.knowledge.ChallengesApi
import io.github.mfabisiak.hubmi.web.knowledge.MaterialsApi
import io.github.mfabisiak.hubmi.web.matching.MatchesApi
import io.github.mfabisiak.hubmi.web.messaging.NotificationsApi
import io.github.mfabisiak.hubmi.web.messaging.ThreadsApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.serialization.json.Json
import kotlin.js.Promise

/**
 * Entry point for the React app. Calls go through the shared `@Resource` routes from `:core`.
 *
 * @param tokenProvider returns the current access token (or null when logged out); evaluated on every request.
 */
@JsExport
class HubApi(
    baseUrl: String,
    tokenProvider: () -> String?,
) {
    private val scope = MainScope()

    private val client =
        HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Resources)
            install(DefaultRequest) {
                url(baseUrl)
                tokenProvider()?.let { bearerAuth(it) }
            }
        }

    val innovations = InnovationsApi(client, scope)
    val challenges = ChallengesApi(client, scope)
    val materials = MaterialsApi(client, scope)
    val matches = MatchesApi(client, scope)
    val ideas = IdeasApi(client, scope)
    val calls = CallsApi(client, scope)
    val applications = ApplicationsApi(client, scope)
    val threads = ThreadsApi(client, scope)
    val notifications = NotificationsApi(client, scope)
    val admin = AdminApi(client, scope)

    fun health(): Promise<ApiResult<HealthJs>> =
        scope.promise {
            client.fetch<Health, HealthResponse>(Health()).toResult { HealthJs(it.status) }
        }

    fun me(): Promise<ApiResult<MeJs>> =
        scope.promise {
            client.fetch<Api.Me, MeResponse>(Api.Me()).toResult {
                MeJs(it.id, it.username, it.email, it.roles.map { role -> role.keycloakName }.toTypedArray())
            }
        }
}
