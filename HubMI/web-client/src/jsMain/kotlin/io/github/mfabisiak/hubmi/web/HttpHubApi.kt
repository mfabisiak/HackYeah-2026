package io.github.mfabisiak.hubmi.web

import io.github.mfabisiak.hubmi.api.Api
import io.github.mfabisiak.hubmi.api.Health
import io.github.mfabisiak.hubmi.api.HealthResponse
import io.github.mfabisiak.hubmi.api.MeResponse
import io.github.mfabisiak.hubmi.web.adaptations.AdaptationsApi
import io.github.mfabisiak.hubmi.web.adaptations.HttpAdaptationsApi
import io.github.mfabisiak.hubmi.web.admin.AdminApi
import io.github.mfabisiak.hubmi.web.admin.HttpAdminApi
import io.github.mfabisiak.hubmi.web.assistant.AssistantApi
import io.github.mfabisiak.hubmi.web.assistant.HttpAssistantApi
import io.github.mfabisiak.hubmi.web.ideas.ApplicationsApi
import io.github.mfabisiak.hubmi.web.ideas.CallsApi
import io.github.mfabisiak.hubmi.web.ideas.HttpApplicationsApi
import io.github.mfabisiak.hubmi.web.ideas.HttpCallsApi
import io.github.mfabisiak.hubmi.web.ideas.HttpIdeasApi
import io.github.mfabisiak.hubmi.web.ideas.IdeasApi
import io.github.mfabisiak.hubmi.web.innovations.HttpInnovationsApi
import io.github.mfabisiak.hubmi.web.innovations.InnovationsApi
import io.github.mfabisiak.hubmi.web.knowledge.ChallengesApi
import io.github.mfabisiak.hubmi.web.knowledge.HttpChallengesApi
import io.github.mfabisiak.hubmi.web.knowledge.HttpMaterialsApi
import io.github.mfabisiak.hubmi.web.knowledge.MaterialsApi
import io.github.mfabisiak.hubmi.web.matching.HttpMatchesApi
import io.github.mfabisiak.hubmi.web.matching.MatchesApi
import io.github.mfabisiak.hubmi.web.messaging.HttpNotificationsApi
import io.github.mfabisiak.hubmi.web.messaging.HttpThreadsApi
import io.github.mfabisiak.hubmi.web.messaging.NotificationsApi
import io.github.mfabisiak.hubmi.web.messaging.ThreadsApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.serialization.json.Json
import kotlin.js.Promise

internal class HttpHubApi(
    baseUrl: String,
    tokenProvider: () -> String?,
) : HubApi {
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

    override val innovations: InnovationsApi = HttpInnovationsApi(client, scope)
    override val challenges: ChallengesApi = HttpChallengesApi(client, scope)
    override val materials: MaterialsApi = HttpMaterialsApi(client, scope)
    override val matches: MatchesApi = HttpMatchesApi(client, scope)
    override val ideas: IdeasApi = HttpIdeasApi(client, scope)
    override val assistant: AssistantApi = HttpAssistantApi(client, scope)
    override val adaptations: AdaptationsApi = HttpAdaptationsApi(client, scope)
    override val calls: CallsApi = HttpCallsApi(client, scope)
    override val applications: ApplicationsApi = HttpApplicationsApi(client, scope)
    override val threads: ThreadsApi = HttpThreadsApi(client, scope)
    override val notifications: NotificationsApi = HttpNotificationsApi(client, scope)
    override val admin: AdminApi = HttpAdminApi(client, scope)

    override fun health(): Promise<ApiResult<HealthJs>> =
        scope.promise {
            client.fetch<Health, HealthResponse>(Health()).toResult { HealthJs(it.status) }
        }

    override fun me(): Promise<ApiResult<MeJs>> =
        scope.promise {
            client.fetch<Api.Me, MeResponse>(Api.Me()).toResult {
                MeJs(it.id, it.username, it.email, it.roles.map { role -> role.keycloakName }.toTypedArray())
            }
        }
}
