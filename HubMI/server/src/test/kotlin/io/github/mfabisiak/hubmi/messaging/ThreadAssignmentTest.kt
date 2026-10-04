package io.github.mfabisiak.hubmi.messaging

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.ReplyTemplateCatalog
import io.github.mfabisiak.hubmi.api.ReplyTemplateDto
import io.github.mfabisiak.hubmi.api.ReplyTemplates
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

class ThreadAssignmentTest {
    private val databaseName = "test-hubmi-assignment"
    private val authorToken = TestSecurityHelper.generateToken(userId = "author-1", username = "jan-kowalski")
    private val annaToken =
        TestSecurityHelper.generateToken(
            userId = "expert-1",
            name = "Anna Nowak",
            roles = setOf(Role.USER, Role.EXPERT),
        )
    private val piotrToken =
        TestSecurityHelper.generateToken(
            userId = "expert-2",
            name = "Piotr Zieliński",
            roles = setOf(Role.USER, Role.EXPERT),
        )
    private val adminToken = TestSecurityHelper.generateToken(userId = "admin-1", roles = setOf(Role.ADMIN))

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

    private suspend fun HttpClient.openThread(): ThreadDto =
        post(Threads()) {
            contentType(ContentType.Application.Json)
            bearerAuth(authorToken)
            setBody(CreateThreadRequest(subject = "Pytanie o nabór", message = "Czy możemy wziąć udział?"))
        }.body()

    private fun assignment(threadId: String) = Threads.ById.Assignment(parent = Threads.ById(id = threadId))

    private suspend fun HttpClient.threadSeenBy(
        token: String,
        threadId: String,
    ): ThreadDto =
        get(Threads()) { bearerAuth(token) }
            .body<Page<ThreadDto>>()
            .items
            .single { it.id == threadId }

    @Test
    fun officialTakesAThreadOverAndEveryoneSeesWhoHandlesIt() =
        withApp { client ->
            val thread = client.openThread()
            assertNull(client.threadSeenBy(annaToken, thread.id).assigneeName)

            val taken = client.put(assignment(thread.id)) { bearerAuth(annaToken) }
            assertEquals(HttpStatusCode.OK, taken.status)
            assertEquals("Anna Nowak", taken.body<ThreadDto>().assigneeName)
            assertTrue(taken.body<ThreadDto>().assignedToMe)

            val seenByAuthor = client.threadSeenBy(authorToken, thread.id)
            assertEquals("Anna Nowak", seenByAuthor.assigneeName)
            assertFalse(seenByAuthor.assignedToMe)

            val seenByColleague = client.threadSeenBy(piotrToken, thread.id)
            assertEquals("Anna Nowak", seenByColleague.assigneeName)
            assertFalse(seenByColleague.assignedToMe)
        }

    @Test
    fun takingOverTwiceIsHarmlessButAnotherOfficialGetsAConflict() =
        withApp { client ->
            val thread = client.openThread()
            client.put(assignment(thread.id)) { bearerAuth(annaToken) }

            assertEquals(HttpStatusCode.OK, client.put(assignment(thread.id)) { bearerAuth(annaToken) }.status)

            val rival = client.put(assignment(thread.id)) { bearerAuth(piotrToken) }
            assertEquals(HttpStatusCode.Conflict, rival.status)
            assertEquals(ErrorCode.CONFLICT, rival.body<ErrorResponse>().code)
            assertEquals("Anna Nowak", client.threadSeenBy(piotrToken, thread.id).assigneeName)
        }

    @Test
    fun onlyTheAssigneeOrAnAdminHandsAThreadBack() =
        withApp { client ->
            val thread = client.openThread()
            client.put(assignment(thread.id)) { bearerAuth(annaToken) }

            assertEquals(
                HttpStatusCode.Forbidden,
                client.delete(assignment(thread.id)) { bearerAuth(piotrToken) }.status,
            )
            assertEquals("Anna Nowak", client.threadSeenBy(annaToken, thread.id).assigneeName)

            val byAssignee = client.delete(assignment(thread.id)) { bearerAuth(annaToken) }
            assertEquals(HttpStatusCode.OK, byAssignee.status)
            assertNull(byAssignee.body<ThreadDto>().assigneeName)

            client.put(assignment(thread.id)) { bearerAuth(piotrToken) }
            assertEquals(HttpStatusCode.OK, client.delete(assignment(thread.id)) { bearerAuth(adminToken) }.status)
            assertNull(client.threadSeenBy(annaToken, thread.id).assigneeName)

            // nobody handles it now, handing it back again changes nothing
            assertEquals(HttpStatusCode.OK, client.delete(assignment(thread.id)) { bearerAuth(annaToken) }.status)
        }

    @Test
    fun assignmentNeedsAnOfficialAndAnExistingThread() =
        withApp { client ->
            val thread = client.openThread()

            assertEquals(HttpStatusCode.Unauthorized, client.put(assignment(thread.id)).status)
            assertEquals(HttpStatusCode.Forbidden, client.put(assignment(thread.id)) { bearerAuth(authorToken) }.status)
            assertEquals(
                HttpStatusCode.Forbidden,
                client.delete(assignment(thread.id)) { bearerAuth(authorToken) }.status,
            )
            assertEquals(HttpStatusCode.NotFound, client.put(assignment("not-an-id")) { bearerAuth(annaToken) }.status)
            assertEquals(
                HttpStatusCode.NotFound,
                client.put(assignment("6ac20009c7d9ca2d97d6bab9")) { bearerAuth(annaToken) }.status,
            )
        }

    @Test
    fun replyTemplatesAreForOfficialsOnly() =
        withApp { client ->
            val templates = client.get(ReplyTemplates()) { bearerAuth(annaToken) }
            assertEquals(HttpStatusCode.OK, templates.status)
            assertEquals(ReplyTemplateCatalog.all, templates.body<List<ReplyTemplateDto>>())

            assertEquals(HttpStatusCode.Forbidden, client.get(ReplyTemplates()) { bearerAuth(authorToken) }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(ReplyTemplates()).status)
        }
}
