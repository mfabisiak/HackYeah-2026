package io.github.mfabisiak.hubmi.web.mock

import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.HealthJs
import io.github.mfabisiak.hubmi.web.HubApi
import io.github.mfabisiak.hubmi.web.MeJs
import io.github.mfabisiak.hubmi.web.adaptations.AdaptationsApi
import io.github.mfabisiak.hubmi.web.adaptations.MockAdaptationsApi
import io.github.mfabisiak.hubmi.web.admin.AdminApi
import io.github.mfabisiak.hubmi.web.admin.MockAdminApi
import io.github.mfabisiak.hubmi.web.assistant.AssistantApi
import io.github.mfabisiak.hubmi.web.assistant.MockAssistantApi
import io.github.mfabisiak.hubmi.web.ideas.ApplicationsApi
import io.github.mfabisiak.hubmi.web.ideas.CallsApi
import io.github.mfabisiak.hubmi.web.ideas.IdeasApi
import io.github.mfabisiak.hubmi.web.ideas.MockApplicationsApi
import io.github.mfabisiak.hubmi.web.ideas.MockCallsApi
import io.github.mfabisiak.hubmi.web.ideas.MockIdeasApi
import io.github.mfabisiak.hubmi.web.innovations.InnovationsApi
import io.github.mfabisiak.hubmi.web.innovations.MockInnovationsApi
import io.github.mfabisiak.hubmi.web.knowledge.ChallengesApi
import io.github.mfabisiak.hubmi.web.knowledge.MaterialsApi
import io.github.mfabisiak.hubmi.web.knowledge.MockChallengesApi
import io.github.mfabisiak.hubmi.web.knowledge.MockMaterialsApi
import io.github.mfabisiak.hubmi.web.matching.MatchesApi
import io.github.mfabisiak.hubmi.web.matching.MockMatchesApi
import io.github.mfabisiak.hubmi.web.messaging.MockNotificationsApi
import io.github.mfabisiak.hubmi.web.messaging.MockThreadsApi
import io.github.mfabisiak.hubmi.web.messaging.NotificationsApi
import io.github.mfabisiak.hubmi.web.messaging.ThreadsApi
import kotlin.js.Promise

/**
 * The demo backend: answers from data kept in the browser (see [MockStore]), signed in as one user who is both a
 * regular user and an admin. Nothing leaves the page.
 */
internal class MockHubApi(
    private val backend: MockBackend,
) : HubApi {
    override val innovations: InnovationsApi = MockInnovationsApi(backend)
    override val challenges: ChallengesApi = MockChallengesApi(backend)
    override val materials: MaterialsApi = MockMaterialsApi(backend)
    override val matches: MatchesApi = MockMatchesApi(backend)
    override val ideas: IdeasApi = MockIdeasApi(backend)
    override val assistant: AssistantApi = MockAssistantApi(backend)
    override val adaptations: AdaptationsApi = MockAdaptationsApi(backend)
    override val calls: CallsApi = MockCallsApi(backend)
    override val applications: ApplicationsApi = MockApplicationsApi(backend)
    override val threads: ThreadsApi = MockThreadsApi(backend)
    override val notifications: NotificationsApi = MockNotificationsApi(backend)
    override val admin: AdminApi = MockAdminApi(backend)

    override fun health(): Promise<ApiResult<HealthJs>> = backend.respond { HealthJs("UP") }

    override fun me(): Promise<ApiResult<MeJs>> =
        backend.respond {
            MeJs(
                DEMO_USER_ID,
                DEMO_USER_NAME,
                DEMO_USER_EMAIL,
                arrayOf(Role.USER.keycloakName, Role.ADMIN.keycloakName),
            )
        }
}

/**
 * The demo implementation of [HubApi]: no server needed. Data starts from the library the server seeds and whatever
 * the visitor adds is kept in the browser's `localStorage`.
 */
@JsExport
fun createMockHubApi(): HubApi = MockHubApi(MockBackend(MockStore(readSavedDb() ?: DemoSeed.initial())))

/** Forgets what the demo has saved in this browser; the next [createMockHubApi] starts from the seed again. */
@JsExport
fun resetMockHubData() = forgetSavedDb()
