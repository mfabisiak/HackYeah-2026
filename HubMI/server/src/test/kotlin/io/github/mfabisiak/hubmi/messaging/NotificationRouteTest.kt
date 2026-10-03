package io.github.mfabisiak.hubmi.messaging

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
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
import java.time.Instant
import kotlin.test.*

class NotificationRouteTest {
    private val databaseName = "test-hubmi-notifications"
    private val adminToken = TestSecurityHelper.generateToken(userId = "admin-1", roles = setOf(Role.ADMIN))
    private val authorToken = TestSecurityHelper.generateToken(userId = "author-1", username = "autor-pomyslu")

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
    fun e2eIdeaSubmissionAndStatusChangeGeneratesNotifications() =
        withApp { client ->
            // 1. Author submits idea
            val createIdeaResponse =
                client.post(Ideas()) {
                    bearerAuth(authorToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Innowacyjna świetlica międzypokoleniowa",
                            essence =
                                "Spotkania seniorów z młodzieżą w celu wymiany umiejętności cyfrowych i rękodzieła.",
                            targetGroups = listOf(TargetGroup.SENIORS, TargetGroup.YOUTH),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Created, createIdeaResponse.status)
            val idea = createIdeaResponse.body<IdeaDto>()

            // Give asynchronous event consumer a moment to persist notification
            delay(150)

            // 2. Admin receives notification about new idea
            val adminNotifications =
                client
                    .get(Notifications()) {
                        bearerAuth(adminToken)
                    }.body<Page<NotificationDto>>()

            assertEquals(1, adminNotifications.total)
            val adminNotif = adminNotifications.items[0]
            assertEquals(NotificationType.IDEA_SUBMITTED, adminNotif.type)
            assertTrue(adminNotif.body.contains("Innowacyjna świetlica międzypokoleniowa"))
            assertFalse(adminNotif.read)

            // Author does NOT receive IDEA_SUBMITTED notification
            val authorNotifsBefore =
                client
                    .get(Notifications()) {
                        bearerAuth(authorToken)
                    }.body<Page<NotificationDto>>()
            assertEquals(0, authorNotifsBefore.total)

            // 3. Admin updates status to IN_REVIEW
            val updateStatusResponse =
                client.patch("/api/admin/ideas/${idea.id}/status") {
                    bearerAuth(adminToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpdateIdeaStatusRequest(
                            status = IdeaStatus.IN_REVIEW,
                            comment = "Wniosek spełnia kryteria formalne, przekazano do oceny merytorycznej.",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, updateStatusResponse.status)

            delay(150)

            // 4. Author now has notification about status change
            val authorNotifsAfter =
                client
                    .get(Notifications()) {
                        bearerAuth(authorToken)
                    }.body<Page<NotificationDto>>()

            assertEquals(1, authorNotifsAfter.total)
            val authorNotif = authorNotifsAfter.items[0]
            assertEquals(NotificationType.IDEA_STATUS_CHANGED, authorNotif.type)
            assertTrue(authorNotif.body.contains("w trakcie oceny"))
            assertFalse(authorNotif.read)

            // 5. Author marks notification as read
            val markReadResponse =
                client.post(Notifications.Read(id = authorNotif.id)) {
                    bearerAuth(authorToken)
                }
            assertEquals(HttpStatusCode.NoContent, markReadResponse.status)

            // 6. Query with unreadOnly = true filters it out
            val authorUnread =
                client
                    .get(Notifications(unreadOnly = true)) {
                        bearerAuth(authorToken)
                    }.body<Page<NotificationDto>>()
            assertEquals(0, authorUnread.total)
        }

    @Test
    fun markingReadIsIsolatedPerCaller() =
        withApp { client ->
            // Seed a role-based admin notification directly
            val notificationRepo =
                NotificationRepository(
                    MongoClient.create(MongoTestEnvironment.connectionString).getDatabase(databaseName),
                )
            val notif =
                notificationRepo
                    .create(
                        NotificationItem(
                            targetRole = Role.ADMIN,
                            type = NotificationType.CALL_PUBLISHED,
                            title = "Nowy nabór",
                            body = "Nabór 2026",
                            createdAt = Instant.now(),
                        ),
                    ).getOrNull()!!

            val admin1 = TestSecurityHelper.generateToken(userId = "admin-1", roles = setOf(Role.ADMIN))
            val admin2 = TestSecurityHelper.generateToken(userId = "admin-2", roles = setOf(Role.ADMIN))

            // Admin1 marks read
            val res1 =
                client.post(Notifications.Read(id = notif.id.toHexString())) {
                    bearerAuth(admin1)
                }
            assertEquals(HttpStatusCode.NoContent, res1.status)

            // Admin1 unread list is empty
            val list1 =
                client
                    .get(Notifications(unreadOnly = true)) {
                        bearerAuth(admin1)
                    }.body<Page<NotificationDto>>()
            assertEquals(0, list1.total)

            // Admin2 unread list STILL has the notification!
            val list2 =
                client
                    .get(Notifications(unreadOnly = true)) {
                        bearerAuth(admin2)
                    }.body<Page<NotificationDto>>()
            assertEquals(1, list2.total)
        }
}
