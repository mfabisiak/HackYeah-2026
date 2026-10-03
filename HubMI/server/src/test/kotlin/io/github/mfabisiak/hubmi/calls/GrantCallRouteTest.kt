package io.github.mfabisiak.hubmi.calls

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.*

class GrantCallRouteTest {
    private val fixedNow = Instant.parse("2026-06-01T12:00:00Z")
    private val testClock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-calls-${System.nanoTime()}",
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
            single<Clock> { testClock }
        }

    private fun ApplicationTestBuilder.createJsonClient() =
        createClient {
            install(Resources)
            install(ContentNegotiation) { json() }
        }

    @Test
    fun getCallsIsPublicAndReturnsCalls() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()

            val response = client.get(Calls())
            assertEquals(HttpStatusCode.OK, response.status)
            val calls = response.body<List<GrantCallDto>>()
            assertNotNull(calls)

            val activeResponse = client.get(Calls.Active())
            assertEquals(HttpStatusCode.OK, activeResponse.status)
            val activeCalls = activeResponse.body<List<GrantCallDto>>()
            assertNotNull(activeCalls)
        }

    @Test
    fun adminCrudForCalls() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val userToken = TestSecurityHelper.generateToken(roles = setOf(Role.USER))
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val opensAt = fixedNow.minus(1, ChronoUnit.DAYS).toString()
            val closesAt = fixedNow.plus(30, ChronoUnit.DAYS).toString()
            val invalidClosesAt = fixedNow.minus(2, ChronoUnit.DAYS).toString()

            val validRequest =
                UpsertCallRequest(
                    title = "Innowacje Społeczne 2026",
                    description = "Ogólnopolski nabór projektów wspierających włączenie społeczne.",
                    opensAt = opensAt,
                    closesAt = closesAt,
                    fields =
                        listOf(
                            CallField(
                                key = "project_name",
                                label = "Nazwa projektu",
                                required = true,
                            ),
                            CallField(
                                key = "budget",
                                label = "Szacowany budżet",
                                required = false,
                            ),
                        ),
                )

            // 1. Regular user gets 403 Forbidden
            val forbiddenPost =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(validRequest)
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenPost.status)

            // 2. Admin creates call with invalid date range -> 400 Bad Request
            val invalidDatePost =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(validRequest.copy(closesAt = invalidClosesAt))
                }
            assertEquals(HttpStatusCode.BadRequest, invalidDatePost.status)

            // 3. Admin creates valid call -> 201 Created
            val createResponse =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(validRequest)
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<GrantCallDto>()
            assertEquals("Innowacje Społeczne 2026", created.title)
            assertEquals(CallStatus.OPEN, created.status)
            assertEquals("/api/calls/${created.id}", createResponse.headers[HttpHeaders.Location])

            // 4. Admin updates call -> 200 OK
            val updatedRequest = validRequest.copy(title = "Innowacje Społeczne 2026 - Zaktualizowany")
            val putResponse =
                client.put(Calls.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(updatedRequest)
                }
            assertEquals(HttpStatusCode.OK, putResponse.status)
            val updated = putResponse.body<GrantCallDto>()
            assertEquals("Innowacje Społeczne 2026 - Zaktualizowany", updated.title)

            // 5. Admin deletes call -> 204 No Content
            val deleteResponse =
                client.delete(Calls.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // 6. Verify deleted -> 404 Not Found
            val getResponse = client.get(Calls.ById(id = created.id))
            assertEquals(HttpStatusCode.NotFound, getResponse.status)
        }

    @Test
    fun submitApplicationValidatesCallStatusFieldsAndIdeaOwnership() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val user1Token = TestSecurityHelper.generateToken(userId = "applicant-1", roles = setOf(Role.USER))
            val user2Token = TestSecurityHelper.generateToken(userId = "applicant-2", roles = setOf(Role.USER))

            // Create an OPEN call
            val openCallResponse =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertCallRequest(
                            title = "Otwarty Nabór",
                            description = "Opis",
                            opensAt = fixedNow.minus(5, ChronoUnit.DAYS).toString(),
                            closesAt = fixedNow.plus(10, ChronoUnit.DAYS).toString(),
                            fields =
                                listOf(
                                    CallField(key = "title", label = "Tytuł", required = true),
                                    CallField(key = "summary", label = "Streszczenie", required = true),
                                    CallField(key = "extra_notes", label = "Dodatkowe uwagi", required = false),
                                ),
                        ),
                    )
                }
            val openCall = openCallResponse.body<GrantCallDto>()

            // Create a CLOSED call
            val closedCallResponse =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertCallRequest(
                            title = "Zamknięty Nabór",
                            description = "Opis",
                            opensAt = fixedNow.minus(20, ChronoUnit.DAYS).toString(),
                            closesAt = fixedNow.minus(5, ChronoUnit.DAYS).toString(),
                            fields = listOf(CallField(key = "title", label = "Tytuł", required = true)),
                        ),
                    )
                }
            val closedCall = closedCallResponse.body<GrantCallDto>()

            // Create idea owned by user2
            val ideaUser2Response =
                client.post(Ideas()) {
                    header(HttpHeaders.Authorization, "Bearer $user2Token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateIdeaRequest(
                            title = "Pomysł Użytkownika 2",
                            essence = "Opis pomysłu U2",
                            targetGroups = listOf(TargetGroup.SENIORS),
                            stage = InnovationStage.IDEA,
                        ),
                    )
                }
            val ideaUser2 = ideaUser2Response.body<IdeaDto>()

            // 1. Submit without authentication -> 401 Unauthorized
            val unauthResponse =
                client.post(Calls.ById.Applications(Calls.ById(id = openCall.id))) {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

            // 2. Submit to closed call -> 409 Conflict
            val closedSubmit =
                client.post(Calls.ById.Applications(Calls.ById(id = closedCall.id))) {
                    header(HttpHeaders.Authorization, "Bearer $user1Token")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            assertEquals(HttpStatusCode.Conflict, closedSubmit.status)

            // 3. Submit with ideaId belonging to someone else -> 403 Forbidden
            val foreignIdeaSubmit =
                client.post(Calls.ById.Applications(Calls.ById(id = openCall.id))) {
                    header(HttpHeaders.Authorization, "Bearer $user1Token")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest(ideaId = ideaUser2.id))
                }
            assertEquals(HttpStatusCode.Forbidden, foreignIdeaSubmit.status)

            // 4. Valid draft creation with prefill -> 201 Created
            val user2Draft =
                client.post(Calls.ById.Applications(Calls.ById(id = openCall.id))) {
                    header(HttpHeaders.Authorization, "Bearer $user2Token")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest(ideaId = ideaUser2.id))
                }
            assertEquals(HttpStatusCode.Created, user2Draft.status)
            val draftDto = user2Draft.body<ApplicationDto>()
            assertEquals(openCall.id, draftDto.callId)
            assertEquals("Pomysł Użytkownika 2", draftDto.title)
            assertEquals("Opis pomysłu U2", draftDto.description)
            assertEquals("/api/applications/${draftDto.id}", user2Draft.headers[HttpHeaders.Location])

            // 5. Declarations endpoint
            val declarationsResponse =
                client.get(
                    Calls.ById.Declarations(
                        parent = Calls.ById(id = openCall.id),
                        applicantType = ApplicantType.INDIVIDUAL,
                    ),
                )
            assertEquals(HttpStatusCode.OK, declarationsResponse.status)
        }
}
