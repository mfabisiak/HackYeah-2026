package io.github.mfabisiak.hubmi.assistant

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.AssistDraftRequest
import io.github.mfabisiak.hubmi.api.AssistEvent
import io.github.mfabisiak.hubmi.api.AssistMode
import io.github.mfabisiak.hubmi.api.AssistRequest
import io.github.mfabisiak.hubmi.api.AssistResponse
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.NoveltyHint
import io.github.mfabisiak.hubmi.api.SseMessage
import io.github.mfabisiak.hubmi.api.SseParser
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
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
import kotlin.test.assertTrue

class AssistRouteTest {
    private val author = TestSecurityHelper.generateToken(userId = "author")
    private val stranger = TestSecurityHelper.generateToken(userId = "stranger")

    private fun config(enabled: Boolean) =
        module {
            single {
                AppConfig(
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-assist-${System.nanoTime()}",
                    assistantEnabled = enabled,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun withApp(
        llm: LlmClient = FakeLlmClient.answering(AssistFixtures.suggestionsAnswer),
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

    private val idea =
        CreateIdeaRequest(
            title = "Klub dla nastolatków",
            essence = "Wieczory z psychologiem w świetlicy wiejskiej",
            targetGroups = listOf(TargetGroup.YOUTH),
            stage = InnovationStage.IDEA,
        )

    private suspend fun HttpClient.assistDraft(
        token: String?,
        mode: AssistMode = AssistMode.EXPAND,
        body: CreateIdeaRequest = idea,
        acceptEvents: Boolean = false,
    ): HttpResponse =
        post(Ideas.Assist()) {
            token?.let(::bearerAuth)
            if (acceptEvents) accept(ContentType.Text.EventStream)
            contentType(ContentType.Application.Json)
            setBody(AssistDraftRequest(mode, body))
        }

    private suspend fun HttpClient.saveIdea(token: String): IdeaDto =
        post(Ideas()) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(idea)
        }.body()

    private suspend fun HttpClient.assistSaved(
        id: String,
        token: String?,
        acceptEvents: Boolean = false,
    ): HttpResponse =
        post(Ideas.ById.Assist(Ideas.ById(id = id))) {
            token?.let(::bearerAuth)
            if (acceptEvents) accept(ContentType.Text.EventStream)
            contentType(ContentType.Application.Json)
            setBody(AssistRequest(AssistMode.EXPAND))
        }

    private fun events(stream: String): List<SseMessage> =
        stream
            .split("\n")
            .runningFold(SseParser.Step(SseParser.INITIAL, null)) { step, line -> step.parser.accept(line) }
            .mapNotNull(SseParser.Step::message)

    @Test
    fun aJsonAnswerCarriesTheSuggestionsAndSaysItIsGenerated() =
        withApp { client ->
            val response = client.assistDraft(author)

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<AssistResponse>()
            assertEquals(AiStatus.OK, body.aiStatus)
            assertEquals(true, body.aiGenerated)
            assertEquals(3, body.suggestions.size)
            assertEquals(NoveltyHint.NEW, body.noveltyHint)
        }

    @Test
    fun anEventStreamSendsTheSimilarPartFirstThenEachSuggestionThenTheWhole() =
        withApp { client ->
            val response = client.assistDraft(author, acceptEvents = true)

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.headers[HttpHeaders.ContentType].orEmpty().startsWith("text/event-stream"))
            val messages = events(response.bodyAsText())
            assertEquals(listOf("similar", "suggestion", "suggestion", "suggestion", "done"), messages.map { it.event })
            val done = DefaultJson.decodeFromString(AssistEvent.serializer(), messages.last().data)
            assertEquals(3, (done as AssistEvent.Done).response.suggestions.size)
        }

    @Test
    fun aSavedIdeaIsAssistedForItsAuthorInBothForms() =
        withApp { client ->
            val saved = client.saveIdea(author)

            assertEquals(AiStatus.OK, client.assistSaved(saved.id, author).body<AssistResponse>().aiStatus)
            assertEquals(
                "done",
                events(client.assistSaved(saved.id, author, acceptEvents = true).bodyAsText()).last().event,
            )
        }

    @Test
    fun aSwitchedOffAssistantStillAnswersAndSaysTheModelIsUnavailable() =
        withApp(enabled = false) { client ->
            val body = client.assistDraft(author).body<AssistResponse>()

            assertEquals(AiStatus.UNAVAILABLE, body.aiStatus)
            assertEquals(emptyList(), body.suggestions)
        }

    @Test
    fun similarNeedsNoModelAtAll() =
        withApp(enabled = false) { client ->
            assertEquals(
                AiStatus.NOT_REQUESTED,
                client.assistDraft(author, AssistMode.SIMILAR).body<AssistResponse>().aiStatus,
            )
        }

    @Test
    fun withoutALoginBothFormsAreRefused() =
        withApp { client ->
            assertEquals(HttpStatusCode.Unauthorized, client.assistDraft(token = null).status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.assistSaved("0123456789abcdef01234567", token = null).status,
            )
        }

    @Test
    fun anInvalidIdeaIsAValidationErrorBeforeAnyStreaming() =
        withApp { client ->
            val response =
                client.assistDraft(
                    author,
                    body = idea.copy(title = " ", targetGroups = emptyList()),
                    acceptEvents = true,
                )

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(ErrorCode.VALIDATION_FAILED, response.body<ErrorResponse>().code)
        }

    @Test
    fun someoneElsesIdeaIsForbiddenAndAnUnknownOneIsNotFound() =
        withApp { client ->
            val saved = client.saveIdea(author)

            assertEquals(HttpStatusCode.Forbidden, client.assistSaved(saved.id, stranger).status)
            assertEquals(HttpStatusCode.NotFound, client.assistSaved("0123456789abcdef01234567", author).status)
        }
}
