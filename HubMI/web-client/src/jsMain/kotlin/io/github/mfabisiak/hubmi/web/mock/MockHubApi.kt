package io.github.mfabisiak.hubmi.web.mock

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

/** [HubApi] of the demo, which also lets the visitor choose whom to sign in as. */
@JsExport
interface DemoHubApi : HubApi {
    /** The accounts of the demo, one per role. */
    fun accounts(): Array<DemoAccountJs>

    /** The account the demo is signed in as; it is remembered across reloads. */
    fun currentAccount(): DemoAccountJs

    /** Signs in as the account of the given `Role` name (`USER`, `EXPERT` or `ADMIN`); `null` for any other name. */
    fun signInAs(role: String): DemoAccountJs?
}

@JsExport
class DemoAccountJs(
    /** `Role` name of the account. */
    val role: String,
    /** What to call the role in the interface, e.g. "Administrator". */
    val label: String,
    val username: String,
    val email: String,
    /** Keycloak names of the roles the account holds. */
    val roles: Array<String>,
)

private fun DemoAccount.toJs(): DemoAccountJs =
    DemoAccountJs(name, label, username, "$username@example.com", roles.map { it.keycloakName }.toTypedArray())

/**
 * The demo backend: answers from data kept in the browser (see [MockStore]) and signs in as one of three accounts that
 * share that data. Nothing leaves the page, and nothing is refused: what each role may see is up to the interface.
 */
internal class MockHubApi(
    private val backend: MockBackend,
) : DemoHubApi {
    override fun accounts(): Array<DemoAccountJs> = DemoAccount.entries.map { it.toJs() }.toTypedArray()

    override fun currentAccount(): DemoAccountJs = backend.account.toJs()

    override fun signInAs(role: String): DemoAccountJs? {
        val account = DemoAccount.entries.firstOrNull { it.name == role }
        account?.let(backend::signIn)
        return account?.toJs()
    }

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

    override fun me(): Promise<ApiResult<MeJs>> = backend.respond { backend.account.toMe() }
}

/**
 * The demo implementation of [HubApi]: no server needed. Data starts from the library the server seeds and whatever
 * the visitor adds is kept in the browser's `localStorage`.
 */
@JsExport
fun createMockHubApi(): DemoHubApi =
    MockHubApi(MockBackend(MockStore(readSavedDb() ?: DemoSeed.initial()), readSavedAccount()))

/** Forgets what the demo has saved in this browser; the next [createMockHubApi] starts from the seed again. */
@JsExport
fun resetMockHubData() = forgetSavedDb()
