package io.github.mfabisiak.hubmi.ideas

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AdminIdeas
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*

class IdeaRouteTest {
    class RecordingEventPublisher : EventPublisher {
        val events = CopyOnWriteArrayList<DomainEvent>()

        override suspend fun publish(event: DomainEvent) {
            events.add(event)
        }
    }

    private val recordingPublisher = RecordingEventPublisher()

    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-ideas-${System.nanoTime()}",
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
            single<EventPublisher> { recordingPublisher }
        }

    private fun ApplicationTestBuilder.createJsonClient() =
        createClient {
            install(Resources)
            install(ContentNegotiation) { json() }
        }

    @Test
    fun postIdeaRequiresAuthentication() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()

            val response =
                client.post(Ideas()) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Test",
                            essence = "Essence",
                            targetGroups = listOf(TargetGroup.SENIORS),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun postIdeaValidatesInput() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val token = TestSecurityHelper.generateToken(roles = setOf(Role.USER))

            val response =
                client.post(Ideas()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "",
                            essence = "",
                            targetGroups = emptyList(),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals(ErrorCode.VALIDATION_FAILED, error.code)
            assertTrue(error.details.any { it.field == "title" })
            assertTrue(error.details.any { it.field == "essence" })
            assertTrue(error.details.any { it.field == "targetGroups" })
        }

    @Test
    fun postIdeaCreatesSubmittedIdeaAndEmitsEvent() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val userId = "user-submitter-1"
            val token = TestSecurityHelper.generateToken(userId = userId, roles = setOf(Role.USER))

            val response =
                client.post(Ideas()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Innowacyjna Opieka",
                            essence = "Platforma wsparcia opiekuńczego dla osób starszych w gminie.",
                            targetGroups = listOf(TargetGroup.SENIORS),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }

            assertEquals(HttpStatusCode.Created, response.status)
            val created = response.body<IdeaDto>()
            assertEquals("Innowacyjna Opieka", created.title)
            assertEquals(IdeaStatus.SUBMITTED, created.status)
            assertNotNull(created.id)
            assertEquals("/api/ideas/${created.id}", response.headers[HttpHeaders.Location])

            val submittedEvent =
                recordingPublisher.events
                    .filterIsInstance<IdeaSubmitted>()
                    .firstOrNull { it.ideaId == created.id }
            assertNotNull(submittedEvent)
            assertEquals(userId, submittedEvent.authorId)
            assertEquals("Innowacyjna Opieka", submittedEvent.title)
        }

    @Test
    fun getMineReturnsOnlyCallerIdeas() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val user1Token = TestSecurityHelper.generateToken(userId = "user-1", roles = setOf(Role.USER))
            val user2Token = TestSecurityHelper.generateToken(userId = "user-2", roles = setOf(Role.USER))

            // User 1 creates 2 ideas
            repeat(2) { index ->
                client.post(Ideas()) {
                    header(HttpHeaders.Authorization, "Bearer $user1Token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Pomysł U1-$index",
                            essence = "Krótki opis pomysłu $index",
                            targetGroups = listOf(TargetGroup.YOUTH),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }
            }

            // User 2 creates 1 idea
            client.post(Ideas()) {
                header(HttpHeaders.Authorization, "Bearer $user2Token")
                contentType(ContentType.Application.Json)
                setBody(
                    CreateIdeaRequest(
                        title = "Pomysł U2-0",
                        essence = "Opis U2",
                        targetGroups = listOf(TargetGroup.RESIDENTS),
                        stage = InnovationStage.PILOT,
                    ),
                )
            }

            // Check User 1 ideas
            val u1Response =
                client.get(Ideas.Mine()) {
                    header(HttpHeaders.Authorization, "Bearer $user1Token")
                }
            assertEquals(HttpStatusCode.OK, u1Response.status)
            val u1Page = u1Response.body<Page<IdeaDto>>()
            assertEquals(2, u1Page.items.size)
            assertTrue(u1Page.items.all { it.title.startsWith("Pomysł U1") })

            // Check User 2 ideas
            val u2Response =
                client.get(Ideas.Mine()) {
                    header(HttpHeaders.Authorization, "Bearer $user2Token")
                }
            assertEquals(HttpStatusCode.OK, u2Response.status)
            val u2Page = u2Response.body<Page<IdeaDto>>()
            assertEquals(1, u2Page.items.size)
            assertEquals("Pomysł U2-0", u2Page.items.first().title)
        }

    @Test
    fun getByIdEnforcesOwnershipAndRoles() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val authorToken = TestSecurityHelper.generateToken(userId = "author-1", roles = setOf(Role.USER))
            val strangerToken = TestSecurityHelper.generateToken(userId = "stranger-1", roles = setOf(Role.USER))
            val adminToken = TestSecurityHelper.generateToken(userId = "admin-1", roles = setOf(Role.ADMIN))
            val expertToken = TestSecurityHelper.generateToken(userId = "expert-1", roles = setOf(Role.EXPERT))

            val createResponse =
                client.post(Ideas()) {
                    header(HttpHeaders.Authorization, "Bearer $authorToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Dostępny Park",
                            essence = "Integracyjny plac zabaw dla dzieci z niepełnosprawnościami.",
                            targetGroups = listOf(TargetGroup.PEOPLE_WITH_DISABILITIES, TargetGroup.FAMILIES),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<IdeaDto>()

            // Author gets 200 OK
            val authorGet =
                client.get(Ideas.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $authorToken")
                }
            assertEquals(HttpStatusCode.OK, authorGet.status)

            // Stranger gets 403 Forbidden
            val strangerGet =
                client.get(Ideas.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $strangerToken")
                }
            assertEquals(HttpStatusCode.Forbidden, strangerGet.status)

            // Admin gets 200 OK
            val adminGet =
                client.get(Ideas.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.OK, adminGet.status)

            // Expert gets 200 OK
            val expertGet =
                client.get(Ideas.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $expertToken")
                }
            assertEquals(HttpStatusCode.OK, expertGet.status)

            // Invalid id format gets 400 Bad Request
            val invalidIdGet =
                client.get(Ideas.ById(id = "not-an-objectid")) {
                    header(HttpHeaders.Authorization, "Bearer $authorToken")
                }
            assertEquals(HttpStatusCode.BadRequest, invalidIdGet.status)

            // Non-existent id gets 404 Not Found
            val nonExistentGet =
                client.get(Ideas.ById(id = "507f1f77bcf86cd799439011")) {
                    header(HttpHeaders.Authorization, "Bearer $authorToken")
                }
            assertEquals(HttpStatusCode.NotFound, nonExistentGet.status)
        }

    @Test
    fun adminStatusTransitionsFollowStateMachine() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val userToken = TestSecurityHelper.generateToken(userId = "user-1", roles = setOf(Role.USER))
            val adminToken = TestSecurityHelper.generateToken(userId = "admin-1", roles = setOf(Role.ADMIN))

            val createResponse =
                client.post(Ideas()) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Weryfikacja Stanów",
                            essence = "Testowanie przejść maszyny stanów fiszki.",
                            targetGroups = listOf(TargetGroup.SENIORS),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }
            val created = createResponse.body<IdeaDto>()
            val ideaId = created.id

            // Regular user cannot update status -> 403
            val userPatch =
                client.patch(AdminIdeas.Status(id = ideaId)) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateIdeaStatusRequest(status = IdeaStatus.IN_REVIEW))
                }
            assertEquals(HttpStatusCode.Forbidden, userPatch.status)

            // Invalid transition from SUBMITTED directly to ACCEPTED -> 409 Conflict
            val invalidPatch =
                client.patch(AdminIdeas.Status(id = ideaId)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateIdeaStatusRequest(status = IdeaStatus.ACCEPTED))
                }
            assertEquals(HttpStatusCode.Conflict, invalidPatch.status)

            // Valid transition: SUBMITTED -> IN_REVIEW -> 200 OK
            val inReviewPatch =
                client.patch(AdminIdeas.Status(id = ideaId)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateIdeaStatusRequest(status = IdeaStatus.IN_REVIEW))
                }
            assertEquals(HttpStatusCode.OK, inReviewPatch.status)
            assertEquals(IdeaStatus.IN_REVIEW, inReviewPatch.body<IdeaDto>().status)

            val statusChangedEvent1 =
                recordingPublisher.events
                    .filterIsInstance<IdeaStatusChanged>()
                    .lastOrNull { it.ideaId == ideaId }
            assertNotNull(statusChangedEvent1)
            assertEquals(IdeaStatus.SUBMITTED, statusChangedEvent1.oldStatus)
            assertEquals(IdeaStatus.IN_REVIEW, statusChangedEvent1.newStatus)

            // IN_REVIEW -> REJECTED without comment -> 400 Bad Request
            val rejectedNoCommentPatch =
                client.patch(AdminIdeas.Status(id = ideaId)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateIdeaStatusRequest(status = IdeaStatus.REJECTED, comment = "   "))
                }
            assertEquals(HttpStatusCode.BadRequest, rejectedNoCommentPatch.status)

            // IN_REVIEW -> REJECTED with comment -> 200 OK
            val rejectedWithCommentPatch =
                client.patch(AdminIdeas.Status(id = ideaId)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpdateIdeaStatusRequest(
                            status = IdeaStatus.REJECTED,
                            comment = "Pomysł nie spełnia kryteriów innowacyjności społecznej.",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, rejectedWithCommentPatch.status)
            val rejectedDto = rejectedWithCommentPatch.body<IdeaDto>()
            assertEquals(IdeaStatus.REJECTED, rejectedDto.status)
            assertEquals("Pomysł nie spełnia kryteriów innowacyjności społecznej.", rejectedDto.adminComment)

            // Attempt transition from terminal REJECTED to IN_REVIEW -> 409 Conflict
            val terminalPatch =
                client.patch(AdminIdeas.Status(id = ideaId)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateIdeaStatusRequest(status = IdeaStatus.IN_REVIEW))
                }
            assertEquals(HttpStatusCode.Conflict, terminalPatch.status)
        }
}
