package io.github.mfabisiak.hubmi.messaging

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.api.Threads
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
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import kotlin.test.*

class ThreadRouteTest {
    private val databaseName = "test-hubmi-threads"
    private val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN), username = "admin-user")
    private val authorToken = TestSecurityHelper.generateToken(userId = "user-author", username = "jan-kowalski")
    private val strangerToken = TestSecurityHelper.generateToken(userId = "user-stranger", username = "anna-nowak")

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
            val client =
                createClient {
                    install(Resources)
                    install(ContentNegotiation) { json() }
                }
            block(client)
        }

    @Test
    fun authorCanCreateThreadAndAdminCanReply() =
        withApp { client ->
            val createResponse =
                client.post(Threads()) {
                    bearerAuth(authorToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateThreadRequest(
                            subject = "Pytanie o nabór dla seniorów",
                            message = "Dzień dobry, czy w naborze mogą wziąć udział grupy nieformalne?",
                        ),
                    )
                }

            assertEquals(HttpStatusCode.Created, createResponse.status)
            val thread = createResponse.body<ThreadDto>()
            assertEquals("Pytanie o nabór dla seniorów", thread.subject)
            assertFalse(thread.unread) // author created it, so not unread for author
            assertNotNull(createResponse.headers[HttpHeaders.Location])

            // Author views messages
            val authorMessages =
                client
                    .get(Threads.ById.Messages(parent = Threads.ById(id = thread.id))) {
                        bearerAuth(authorToken)
                    }.body<List<MessageDto>>()

            assertEquals(1, authorMessages.size)
            assertEquals("jan-kowalski", authorMessages[0].authorName)
            assertEquals(ParticipantRole.AUTHOR, authorMessages[0].authorRole)
            assertEquals("Dzień dobry, czy w naborze mogą wziąć udział grupy nieformalne?", authorMessages[0].text)

            // Admin lists threads and sees it as unread
            val adminThreads =
                client
                    .get(Threads()) {
                        bearerAuth(adminToken)
                    }.body<Page<ThreadDto>>()

            assertEquals(1, adminThreads.total)
            assertTrue(adminThreads.items[0].unread)

            // Admin opens thread -> reading messages marks it read
            val messagesBeforeReply =
                client
                    .get(Threads.ById.Messages(parent = Threads.ById(id = thread.id))) {
                        bearerAuth(adminToken)
                    }.body<List<MessageDto>>()
            assertEquals(1, messagesBeforeReply.size)

            val adminThreadsAfterRead =
                client
                    .get(Threads()) {
                        bearerAuth(adminToken)
                    }.body<Page<ThreadDto>>()
            assertFalse(adminThreadsAfterRead.items[0].unread)

            // Admin replies
            val replyResponse =
                client.post(Threads.ById.Messages(parent = Threads.ById(id = thread.id))) {
                    bearerAuth(adminToken)
                    contentType(ContentType.Application.Json)
                    setBody(PostMessageRequest("Tak, grupy nieformalne 1-5 osób są uprawnione."))
                }

            assertEquals(HttpStatusCode.Created, replyResponse.status)
            val reply = replyResponse.body<MessageDto>()
            assertEquals("admin-user", reply.authorName)
            assertEquals(ParticipantRole.ADMIN, reply.authorRole)

            // Author checks threads: now unread for author!
            val authorThreads =
                client
                    .get(Threads()) {
                        bearerAuth(authorToken)
                    }.body<Page<ThreadDto>>()
            assertTrue(authorThreads.items[0].unread)
        }

    @Test
    fun strangerCannotReadOrPostToOtherUsersThread() =
        withApp { client ->
            val createResponse =
                client.post(Threads()) {
                    bearerAuth(authorToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateThreadRequest(
                            subject = "Poufna konsultacja",
                            message = "Szczegóły innowacji",
                        ),
                    )
                }
            val threadId = createResponse.body<ThreadDto>().id

            // Stranger cannot view messages (returns 404 to avoid leaking existence)
            val getResponse =
                client.get(Threads.ById.Messages(parent = Threads.ById(id = threadId))) {
                    bearerAuth(strangerToken)
                }
            assertEquals(HttpStatusCode.NotFound, getResponse.status)

            // Stranger cannot post message (returns 404)
            val postResponse =
                client.post(Threads.ById.Messages(parent = Threads.ById(id = threadId))) {
                    bearerAuth(strangerToken)
                    contentType(ContentType.Application.Json)
                    setBody(PostMessageRequest("Wtrącam się w cudzy wątek"))
                }
            assertEquals(HttpStatusCode.NotFound, postResponse.status)

            // Stranger listing threads sees empty list
            val strangerThreads =
                client
                    .get(Threads()) {
                        bearerAuth(strangerToken)
                    }.body<Page<ThreadDto>>()
            assertEquals(0, strangerThreads.total)
        }

    @Test
    fun validationRejectsBlankOrOversizedFields() =
        withApp { client ->
            val blankResponse =
                client.post(Threads()) {
                    bearerAuth(authorToken)
                    contentType(ContentType.Application.Json)
                    setBody(CreateThreadRequest(subject = "   ", message = ""))
                }
            assertEquals(HttpStatusCode.BadRequest, blankResponse.status)
            val blankErr = blankResponse.body<ErrorResponse>()
            assertEquals(ErrorCode.VALIDATION_FAILED, blankErr.code)

            val tooLongMessage = "a".repeat(2001)
            val oversizedResponse =
                client.post(Threads()) {
                    bearerAuth(authorToken)
                    contentType(ContentType.Application.Json)
                    setBody(CreateThreadRequest(subject = "Temat", message = tooLongMessage))
                }
            assertEquals(HttpStatusCode.BadRequest, oversizedResponse.status)
        }
}
