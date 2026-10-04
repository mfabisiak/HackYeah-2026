package io.github.mfabisiak.hubmi.web

import io.github.mfabisiak.hubmi.web.adaptations.AdaptationsApi
import io.github.mfabisiak.hubmi.web.admin.AdminApi
import io.github.mfabisiak.hubmi.web.assistant.AssistantApi
import io.github.mfabisiak.hubmi.web.ideas.ApplicationsApi
import io.github.mfabisiak.hubmi.web.ideas.CallsApi
import io.github.mfabisiak.hubmi.web.ideas.IdeasApi
import io.github.mfabisiak.hubmi.web.innovations.InnovationsApi
import io.github.mfabisiak.hubmi.web.knowledge.ChallengesApi
import io.github.mfabisiak.hubmi.web.knowledge.MaterialsApi
import io.github.mfabisiak.hubmi.web.matching.MatchesApi
import io.github.mfabisiak.hubmi.web.messaging.NotificationsApi
import io.github.mfabisiak.hubmi.web.messaging.ThreadsApi
import kotlin.js.Promise

/**
 * What the React app sees of the backend. There are two implementations: [createHttpHubApi] talks to the server through
 * the shared `@Resource` routes from `:core`, [createMockHubApi] answers from data kept in the browser (for the demo).
 */
@JsExport
interface HubApi {
    val innovations: InnovationsApi
    val challenges: ChallengesApi
    val materials: MaterialsApi
    val matches: MatchesApi
    val ideas: IdeasApi
    val assistant: AssistantApi
    val adaptations: AdaptationsApi
    val calls: CallsApi
    val applications: ApplicationsApi
    val threads: ThreadsApi
    val notifications: NotificationsApi
    val admin: AdminApi

    fun health(): Promise<ApiResult<HealthJs>>

    fun me(): Promise<ApiResult<MeJs>>
}

/**
 * The client of the real backend.
 *
 * @param tokenProvider returns the current access token (or null when logged out); evaluated on every request.
 */
@JsExport
fun createHttpHubApi(
    baseUrl: String,
    tokenProvider: () -> String?,
): HubApi = HttpHubApi(baseUrl, tokenProvider)
