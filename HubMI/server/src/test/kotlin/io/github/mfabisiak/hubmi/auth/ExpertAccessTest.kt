package io.github.mfabisiak.hubmi.auth

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AdminIdeas
import io.github.mfabisiak.hubmi.api.AdminSummary
import io.github.mfabisiak.hubmi.api.AdminSummaryDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.AdminTestRequests
import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.CreateTestRequest
import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.api.UpdateTestRequestStatusRequest
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import kotlin.test.*

/** The expert is a ROPS official: moderates submissions and answers residents, but sees no trends. */
class ExpertAccessTest {
    private val databaseName = "test-hubmi-expert"
    private val adminToken = TestSecurityHelper.generateToken(userId = "admin-1", roles = setOf(Role.ADMIN))
    private val expertToken =
        TestSecurityHelper.generateToken(
            userId = "expert-1",
            username = "expert",
            name = "Anna Nowak",
            roles = setOf(Role.USER, Role.EXPERT),
        )
    private val authorToken = TestSecurityHelper.generateToken(userId = "author-1", username = "jan-kowalski")

    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = databaseName,
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun withApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            MongoClient.create(MongoTestEnvironment.connectionString).use {
                runBlocking { it.getDatabase(databaseName).drop() }
            }
            application { module(testModule) }
            block(
                createClient {
                    install(Resources)
                    install(ContentNegotiation) { json() }
                },
            )
        }

    private suspend fun HttpClient.submitIdea(): IdeaDto =
        post(Ideas()) {
            contentType(ContentType.Application.Json)
            bearerAuth(authorToken)
            setBody(
                CreateIdeaRequest(
                    title = "Sąsiedzka pomoc seniorom",
                    essence = "Wolontariusze pomagają seniorom w codziennych sprawach.",
                    targetGroups = listOf(TargetGroup.SENIORS),
                    stage = InnovationStage.IDEA,
                ),
            )
        }.body()

    private suspend fun HttpClient.openThread(): ThreadDto =
        post(Threads()) {
            contentType(ContentType.Application.Json)
            bearerAuth(authorToken)
            setBody(CreateThreadRequest(subject = "Pytanie o nabór", message = "Czy możemy wziąć udział?"))
        }.body()

    @Test
    fun expertSeesTheSummaryButNotTheTrends() =
        withApp { client ->
            assertEquals(
                HttpStatusCode.OK,
                client.get(AdminSummary()) { bearerAuth(expertToken) }.status,
            )
            assertEquals(
                HttpStatusCode.Forbidden,
                client.get(AdminTrends()) { bearerAuth(expertToken) }.status,
            )
        }

    @Test
    fun expertModeratesIdeasWhileAnOrdinaryUserCannot() =
        withApp { client ->
            val idea = client.submitIdea()

            val queue = client.get(AdminIdeas()) { bearerAuth(expertToken) }
            assertEquals(HttpStatusCode.OK, queue.status)
            assertEquals(listOf(idea.id), queue.body<Page<IdeaDto>>().items.map(IdeaDto::id))

            val decided =
                client.patch(AdminIdeas.Status(id = idea.id)) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(expertToken)
                    setBody(UpdateIdeaStatusRequest(status = IdeaStatus.IN_REVIEW))
                }
            assertEquals(HttpStatusCode.OK, decided.status)
            assertEquals(IdeaStatus.IN_REVIEW, decided.body<IdeaDto>().status)

            assertEquals(
                HttpStatusCode.Forbidden,
                client.get(AdminIdeas()) { bearerAuth(authorToken) }.status,
            )
        }

    @Test
    fun expertDecidesOnTestRequests() =
        withApp { client ->
            val innovation =
                client
                    .post(Innovations()) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(
                            UpsertInnovationRequest(
                                title = "Klub sąsiedzki",
                                summary = "Spotkania sąsiadów.",
                                description = "Cotygodniowe spotkania sąsiadów w świetlicy osiedlowej.",
                                areas = listOf(SocialArea.LONELINESS),
                                targetGroups = listOf(TargetGroup.RESIDENTS),
                                stage = InnovationStage.PILOT,
                            ),
                        )
                    }.body<InnovationDto>()
            client.put(Innovations.ById.TestRequest(Innovations.ById(id = innovation.id))) {
                contentType(ContentType.Application.Json)
                bearerAuth(authorToken)
                setBody(CreateTestRequest("Chcemy przetestować"))
            }

            val requests =
                client.get(AdminTestRequests()) { bearerAuth(expertToken) }.body<Page<AdminTestRequestDto>>()
            assertEquals(1, requests.total)

            val decided =
                client.patch(AdminTestRequests.Status(id = requests.items.single().id)) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(expertToken)
                    setBody(UpdateTestRequestStatusRequest(TestRequestStatus.ACCEPTED))
                }
            assertEquals(TestRequestStatus.ACCEPTED, decided.body<AdminTestRequestDto>().status)
        }

    @Test
    fun expertAnswersAnyThreadAsAnOfficialUnderTheirOwnName() =
        withApp { client ->
            val thread = client.openThread()
            val messages = Threads.ById.Messages(parent = Threads.ById(id = thread.id))

            assertEquals(
                1,
                client.get(AdminSummary()) { bearerAuth(adminToken) }.body<AdminSummaryDto>().pendingThreads,
            )
            assertEquals(1, client.get(Threads()) { bearerAuth(expertToken) }.body<Page<ThreadDto>>().total)

            val reply =
                client.post(messages) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(expertToken)
                    setBody(PostMessageRequest("Tak, grupy nieformalne mogą składać wnioski."))
                }
            assertEquals(HttpStatusCode.Created, reply.status)
            val message = reply.body<MessageDto>()
            assertEquals("Anna Nowak", message.authorName)
            assertEquals(ParticipantRole.EXPERT, message.authorRole)

            val authorView = client.get(messages) { bearerAuth(authorToken) }.body<List<MessageDto>>()
            assertEquals(listOf(ParticipantRole.AUTHOR, ParticipantRole.EXPERT), authorView.map(MessageDto::authorRole))

            assertEquals(
                0,
                client.get(AdminSummary()) { bearerAuth(adminToken) }.body<AdminSummaryDto>().pendingThreads,
            )
        }

    @Test
    fun ordinaryUserStillCannotReadSomeoneElsesThread() =
        withApp { client ->
            val thread = client.openThread()
            val stranger = TestSecurityHelper.generateToken(userId = "stranger-1")

            val response =
                client.get(Threads.ById.Messages(parent = Threads.ById(id = thread.id))) { bearerAuth(stranger) }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun expertGetsTheSameStaffNotificationsAsAdmin() =
        withApp { client ->
            client.submitIdea()
            client.openThread()

            // The notification consumer persists events asynchronously.
            delay(150)

            suspend fun typesFor(token: String): Set<NotificationType> =
                client
                    .get(Notifications()) { bearerAuth(token) }
                    .body<Page<NotificationDto>>()
                    .items
                    .map(NotificationDto::type)
                    .toSet()

            val expected = setOf(NotificationType.IDEA_SUBMITTED, NotificationType.MESSAGE_RECEIVED)
            assertEquals(expected, typesFor(expertToken))
            assertEquals(expected, typesFor(adminToken))
            assertTrue(typesFor(authorToken).isEmpty())
        }
}
