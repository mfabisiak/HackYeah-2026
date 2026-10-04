package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationEvent
import io.github.mfabisiak.hubmi.api.AdaptationResponse
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.Adaptations
import io.github.mfabisiak.hubmi.api.AdminAdaptations
import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.SseMessage
import io.github.mfabisiak.hubmi.api.SseParser
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpdateAdaptationStatusRequest
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.assistant.FakeLlmClient
import io.github.mfabisiak.hubmi.assistant.LlmClient
import io.github.mfabisiak.hubmi.assistant.LlmError
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdaptationRouteTest {
    private val author = TestSecurityHelper.generateToken(userId = "author")
    private val stranger = TestSecurityHelper.generateToken(userId = "stranger")
    private val admin = TestSecurityHelper.generateToken(userId = "admin", roles = setOf(Role.ADMIN))

    private fun config(enabled: Boolean) =
        module {
            single {
                AppConfig(
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-adaptations-${System.nanoTime()}",
                    assistantEnabled = enabled,
                    llmModel = MODEL,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun withApp(
        llm: LlmClient = FakeLlmClient.answering(AdaptationFixtures.planAnswer()),
        enabled: Boolean = true,
        block: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
    ) = testApplication {
        application { module(config(enabled), module { single<LlmClient> { llm } }) }
        block(
            createClient {
                install(Resources)
                install(ContentNegotiation) { json() }
            },
        )
    }

    private suspend fun HttpClient.createInnovation(): InnovationDto =
        post(Innovations()) {
            bearerAuth(admin)
            contentType(ContentType.Application.Json)
            setBody(
                UpsertInnovationRequest(
                    title = "Transport door-to-door",
                    summary = "Dowóz osób z ograniczoną mobilnością do przychodni",
                    description = "Gminny system przewozu realizowany przez wolontariuszy.",
                    areas = listOf(SocialArea.SERVICE_ACCESS),
                    targetGroups = listOf(TargetGroup.SENIORS),
                    stage = InnovationStage.IMPLEMENTED,
                ),
            )
        }.body()

    private suspend fun HttpClient.adapt(
        innovationId: String,
        token: String?,
        institution: InstitutionProfile = AdaptationFixtures.institution,
        acceptEvents: Boolean = false,
    ): HttpResponse =
        post(Innovations.ById.Adaptations(Innovations.ById(id = innovationId))) {
            token?.let(::bearerAuth)
            if (acceptEvents) accept(ContentType.Text.EventStream)
            contentType(ContentType.Application.Json)
            setBody(institution)
        }

    private suspend fun HttpClient.adaptAndStore(): AdaptationDto {
        val innovation = createInnovation()
        return assertNotNull(adapt(innovation.id, author).body<AdaptationResponse>().adaptation)
    }

    private suspend fun HttpClient.review(
        id: String,
        status: AdaptationStatus,
        comment: String? = null,
        token: String = admin,
    ): HttpResponse =
        patch(AdminAdaptations.Status(id = id)) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateAdaptationStatusRequest(status, comment))
        }

    private fun events(stream: String): List<SseMessage> =
        stream
            .split("\n")
            .runningFold(SseParser.Step(SseParser.INITIAL, null)) { step, line -> step.parser.accept(line) }
            .mapNotNull(SseParser.Step::message)

    @Test
    fun aPlanIsWrittenStoredAndMarkedAsAiForReview() =
        withApp { client ->
            val innovation = client.createInnovation()

            val response = client.adapt(innovation.id, author)

            assertEquals(HttpStatusCode.Created, response.status)
            val body = response.body<AdaptationResponse>()
            assertEquals(AiStatus.OK, body.aiStatus)
            val adaptation = assertNotNull(body.adaptation)
            assertEquals("/api/adaptations/${adaptation.id}", response.headers[HttpHeaders.Location])
            assertEquals(true, adaptation.aiGenerated)
            assertEquals(MODEL, adaptation.model)
            assertEquals(AdaptationStatus.PENDING_REVIEW, adaptation.status)
            assertEquals(innovation.title, adaptation.innovationTitle)
            assertEquals(AdaptationFixtures.institution, adaptation.institution)
            assertEquals(4, adaptation.plan.steps.size)
            assertEquals(false, adaptation.plan.exceedsBudget)
        }

    @Test
    fun aPlanThatCostsMoreThanTheBudgetIsStoredButMarked() =
        withApp(FakeLlmClient.answering(AdaptationFixtures.planAnswer(costPln = 90_000))) { client ->
            val body = client.adapt(client.createInnovation().id, author).body<AdaptationResponse>()

            assertEquals(true, body.adaptation?.plan?.exceedsBudget)
        }

    @Test
    fun anEventStreamSendsTheStepsOneByOneThenTheStoredPlan() =
        withApp { client ->
            val response = client.adapt(client.createInnovation().id, author, acceptEvents = true)

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.headers[HttpHeaders.ContentType].orEmpty().startsWith("text/event-stream"))
            val messages = events(response.bodyAsText())
            assertEquals(listOf("step", "step", "step", "step", "done"), messages.map { it.event })
            val done = DefaultJson.decodeFromString(AdaptationEvent.serializer(), messages.last().data)
            assertIs<AdaptationEvent.Done>(done)
            assertEquals(AiStatus.OK, done.response.aiStatus)
            assertNotNull(done.response.adaptation)
        }

    @Test
    fun aModelThatIsDownGivesAStatusAndStoresNothing() =
        withApp(FakeLlmClient.failing(LlmError.Unavailable(detail = "down"))) { client ->
            val response = client.adapt(client.createInnovation().id, author)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(AdaptationResponse(AiStatus.UNAVAILABLE, null), response.body())
            assertEquals(0, client.get(Adaptations.Mine()) { bearerAuth(author) }.body<Page<AdaptationDto>>().total)
        }

    @Test
    fun aPlanThatFailsTheChecksIsInvalidOutputAndStoresNothing() =
        withApp(
            FakeLlmClient.answering(
                """{"serviceForm":"x","steps":[],"requiredResources":[],"risks":[],"estimatedCostPln":1}""",
            ),
        ) { client ->
            val body = client.adapt(client.createInnovation().id, author).body<AdaptationResponse>()

            assertEquals(AdaptationResponse(AiStatus.INVALID_OUTPUT, null), body)
        }

    @Test
    fun aSwitchedOffAssistantIsUnavailableNotAnError() =
        withApp(enabled = false) { client ->
            assertEquals(
                AiStatus.UNAVAILABLE,
                client.adapt(client.createInnovation().id, author).body<AdaptationResponse>().aiStatus,
            )
        }

    @Test
    fun withoutALoginAnUnknownInnovationOrABadInstitutionItIsAnError() =
        withApp { client ->
            val innovation = client.createInnovation()
            val bad = AdaptationFixtures.institution.copy(staffCount = -1, budgetPln = 5, context = " ")

            assertEquals(HttpStatusCode.Unauthorized, client.adapt(innovation.id, token = null).status)
            assertEquals(HttpStatusCode.NotFound, client.adapt("0123456789abcdef01234567", author).status)
            assertEquals(HttpStatusCode.BadRequest, client.adapt("not-an-id", author).status)
            val invalid = client.adapt(innovation.id, author, bad, acceptEvents = true)
            assertEquals(HttpStatusCode.BadRequest, invalid.status)
            assertEquals(
                setOf("staffCount", "budgetPln", "context"),
                invalid
                    .body<ErrorResponse>()
                    .details
                    .map { it.field }
                    .toSet(),
            )
        }

    @Test
    fun anAuthorSeesTheirOwnPlansAndAStrangerDoesNot() =
        withApp { client ->
            val stored = client.adaptAndStore()

            val mine = client.get(Adaptations.Mine()) { bearerAuth(author) }.body<Page<AdaptationDto>>()
            val others = client.get(Adaptations.Mine()) { bearerAuth(stranger) }.body<Page<AdaptationDto>>()
            assertEquals(listOf(stored.id), mine.items.map { it.id })
            assertEquals(emptyList(), others.items)
            assertEquals(HttpStatusCode.OK, client.get(Adaptations.ById(id = stored.id)) { bearerAuth(author) }.status)
            assertEquals(HttpStatusCode.OK, client.get(Adaptations.ById(id = stored.id)) { bearerAuth(admin) }.status)
            assertEquals(
                HttpStatusCode.Forbidden,
                client.get(Adaptations.ById(id = stored.id)) { bearerAuth(stranger) }.status,
            )
            assertEquals(
                HttpStatusCode.NotFound,
                client.get(Adaptations.ById(id = "0123456789abcdef01234567")) { bearerAuth(author) }.status,
            )
        }

    @Test
    fun onlyAnAdminSeesTheReviewQueue() =
        withApp { client ->
            val stored = client.adaptAndStore()

            assertEquals(HttpStatusCode.Forbidden, client.get(AdminAdaptations()) { bearerAuth(author) }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(AdminAdaptations()).status)
            val queue = client.get(AdminAdaptations(status = AdaptationStatus.PENDING_REVIEW)) { bearerAuth(admin) }
            assertEquals(listOf(stored.id), queue.body<Page<AdaptationDto>>().items.map { it.id })
            val approved = client.get(AdminAdaptations(status = AdaptationStatus.APPROVED)) { bearerAuth(admin) }
            assertEquals(0, approved.body<Page<AdaptationDto>>().total)
        }

    @Test
    fun anAdminApprovesOncePlansAndALaterChangeIsAConflict() =
        withApp { client ->
            val stored = client.adaptAndStore()

            val approved = client.review(stored.id, AdaptationStatus.APPROVED, comment = "Dobry plan")
            assertEquals(HttpStatusCode.OK, approved.status)
            assertEquals(AdaptationStatus.APPROVED, approved.body<AdaptationDto>().status)
            assertEquals("Dobry plan", approved.body<AdaptationDto>().adminComment)
            val again = client.review(stored.id, AdaptationStatus.REJECTED, comment = "Jednak nie")
            assertEquals(HttpStatusCode.Conflict, again.status)
            assertEquals(ErrorCode.CONFLICT, again.body<ErrorResponse>().code)
        }

    @Test
    fun rejectingNeedsACommentAndOnlyAnAdminMayReview() =
        withApp { client ->
            val stored = client.adaptAndStore()

            assertEquals(HttpStatusCode.BadRequest, client.review(stored.id, AdaptationStatus.REJECTED).status)
            assertEquals(HttpStatusCode.BadRequest, client.review(stored.id, AdaptationStatus.REJECTED, " ").status)
            assertEquals(
                HttpStatusCode.Forbidden,
                client.review(stored.id, AdaptationStatus.APPROVED, token = author).status,
            )
            assertEquals(HttpStatusCode.Conflict, client.review(stored.id, AdaptationStatus.PENDING_REVIEW).status)
            assertEquals(
                HttpStatusCode.NotFound,
                client.review("0123456789abcdef01234567", AdaptationStatus.APPROVED).status,
            )
            assertEquals(HttpStatusCode.OK, client.review(stored.id, AdaptationStatus.REJECTED, "Za drogo").status)
        }

    private companion object {
        const val MODEL = "test-model"
    }
}
