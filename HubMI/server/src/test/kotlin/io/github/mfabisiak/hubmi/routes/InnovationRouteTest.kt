package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.bson.types.ObjectId
import org.koin.dsl.module
import kotlin.test.*

class InnovationRouteTest {
    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-innovations",
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun ApplicationTestBuilder.createJsonClient() =
        createClient {
            install(Resources)
            install(ContentNegotiation) { json() }
        }

    private fun sampleRequest(
        title: String = "Innowacyjny Klub Sąsiedzki",
        area: SocialArea = SocialArea.AGING,
        targetGroup: TargetGroup = TargetGroup.SENIORS,
    ) = UpsertInnovationRequest(
        title = title,
        summary = "Krótkie podsumowanie innowacji wspierającej seniorów.",
        description = "Szczegółowy opis innowacji społecznej wdrażanej w środowisku lokalnym.",
        areas = listOf(area),
        targetGroups = listOf(targetGroup),
        stage = InnovationStage.PILOT,
        region = "Małopolska",
        mediaUrls = listOf("https://example.com/video"),
    )

    @Test
    fun getInnovationsReturnsOk() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()

            val response = client.get(Innovations())
            assertEquals(HttpStatusCode.OK, response.status)
            val page = response.body<Page<InnovationSummary>>()
            assertNotNull(page)
        }

    @Test
    fun postInnovationRequiresAdminRole() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val request = sampleRequest()

            // 1. Without token -> 401
            val unauthResponse =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

            // 2. With user role -> 403
            val userToken = TestSecurityHelper.generateToken(roles = setOf(Role.USER))
            val forbiddenResponse =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(userToken)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)

            // 3. With admin role -> 201 Created
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val successResponse =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.Created, successResponse.status)
            val created = successResponse.body<InnovationDto>()
            assertEquals(request.title, created.title)
            assertEquals(request.areas, created.areas)
            assertNotNull(created.id)
        }

    @Test
    fun postInnovationValidationErrors() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val invalidRequest =
                UpsertInnovationRequest(
                    title = "AB", // too short (min 3)
                    summary = "", // blank
                    description = "", // blank
                    areas = emptyList(), // required
                    targetGroups = emptyList(), // required
                    stage = InnovationStage.IDEA,
                    mediaUrls = listOf("not-a-valid-url"),
                )

            val response =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(invalidRequest)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals(ErrorCode.VALIDATION_FAILED, error.code)
            assertTrue(error.details.isNotEmpty())
            assertTrue(error.details.any { it.field == "title" })
            assertTrue(error.details.any { it.field == "summary" })
            assertTrue(error.details.any { it.field == "description" })
            assertTrue(error.details.any { it.field == "areas" })
            assertTrue(error.details.any { it.field == "targetGroups" })
            assertTrue(error.details.any { it.field.startsWith("mediaUrls") })
        }

    @Test
    fun getByIdAndFilterInnovations() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val uniqueTitle = "Cyfrowy Senior ${System.nanoTime()}"
            val request = sampleRequest(title = uniqueTitle, area = SocialArea.DIGITAL_EXCLUSION)
            val createResponse =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<InnovationDto>()

            // 1. Get by ID -> 200
            val getResponse = client.get(Innovations.ById(id = created.id))
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val fetched = getResponse.body<InnovationDto>()
            assertEquals(created.id, fetched.id)
            assertEquals(uniqueTitle, fetched.title)

            // 2. Filter by area -> should contain
            val filterResponse = client.get(Innovations(area = SocialArea.DIGITAL_EXCLUSION))
            assertEquals(HttpStatusCode.OK, filterResponse.status)
            val filteredPage = filterResponse.body<Page<InnovationSummary>>()
            assertTrue(filteredPage.items.any { it.id == created.id })

            // 3. Filter by search query q -> should contain
            val searchResponse = client.get(Innovations(q = uniqueTitle))
            assertEquals(HttpStatusCode.OK, searchResponse.status)
            val searchPage = searchResponse.body<Page<InnovationSummary>>()
            assertTrue(searchPage.items.any { it.id == created.id })

            // 4. Nonexistent ID -> 404
            val nonExistentId = ObjectId().toHexString()
            val notFoundResponse = client.get(Innovations.ById(id = nonExistentId))
            assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)
        }

    @Test
    fun putAndUpdateInnovation() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val createResponse =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(sampleRequest(title = "Przed Aktualizacją ${System.nanoTime()}"))
                }
            val created = createResponse.body<InnovationDto>()

            // Update
            val updatedTitle = "Po Aktualizacji ${System.nanoTime()}"
            val updateRequest = sampleRequest(title = updatedTitle, area = SocialArea.MENTAL_HEALTH)
            val putResponse =
                client.put(Innovations.ById(id = created.id)) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(updateRequest)
                }
            assertEquals(HttpStatusCode.OK, putResponse.status)
            val updatedDto = putResponse.body<InnovationDto>()
            assertEquals(updatedTitle, updatedDto.title)
            assertEquals(listOf(SocialArea.MENTAL_HEALTH), updatedDto.areas)

            // Fetch to verify
            val getResponse = client.get(Innovations.ById(id = created.id))
            val fetched = getResponse.body<InnovationDto>()
            assertEquals(updatedTitle, fetched.title)
        }

    @Test
    fun deleteInnovationSoftDeletes() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val createResponse =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(sampleRequest(title = "Do Usunięcia ${System.nanoTime()}"))
                }
            val created = createResponse.body<InnovationDto>()

            // User cannot delete -> 403
            val userToken = TestSecurityHelper.generateToken(roles = setOf(Role.USER))
            val forbiddenDelete =
                client.delete(Innovations.ById(id = created.id)) {
                    bearerAuth(userToken)
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenDelete.status)

            // Admin deletes -> 204
            val successDelete =
                client.delete(Innovations.ById(id = created.id)) {
                    bearerAuth(adminToken)
                }
            assertEquals(HttpStatusCode.NoContent, successDelete.status)

            // Subsequent GET -> 404
            val afterDeleteGet = client.get(Innovations.ById(id = created.id))
            assertEquals(HttpStatusCode.NotFound, afterDeleteGet.status)
        }

    @Test
    fun malformedInputYieldsBadRequestInsteadOfServerError() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            assertEquals(HttpStatusCode.BadRequest, client.get("/api/innovations?area=NIE_ISTNIEJE").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/api/innovations?page=abc").status)
            assertEquals(HttpStatusCode.BadRequest, client.get(Innovations.ById(id = "nie-object-id")).status)

            val brokenBody =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody("{ \"title\": ")
                }
            assertEquals(HttpStatusCode.BadRequest, brokenBody.status)
            assertEquals(ErrorCode.VALIDATION_FAILED, brokenBody.body<ErrorResponse>().code)
        }

    @Test
    fun putNormalisesInputTheSameWayAsPost() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val created =
                client
                    .post(Innovations()) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(sampleRequest(title = "Normalizacja ${System.nanoTime()}"))
                    }.body<InnovationDto>()

            val updated =
                client
                    .put(Innovations.ById(id = created.id)) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(
                            sampleRequest(title = "  Po normalizacji ${System.nanoTime()}  ")
                                .copy(mediaUrls = listOf("  https://example.com/x  "), region = "  Kraków "),
                        )
                    }.body<InnovationDto>()

            assertEquals(listOf("https://example.com/x"), updated.mediaUrls)
            assertEquals("Kraków", updated.region)
            assertFalse(updated.title.startsWith(" "))
        }

    @Test
    fun applicationFormSectionsAreStoredUpdatedAndClearedOnPut() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val withSections =
                sampleRequest(title = "Z sekcjami ${System.nanoTime()}").copy(
                    innovativeness = "  Nowe podejście do transportu  ",
                    problemDiagnosis = "Starsi mieszkańcy wsi nie mają jak dojechać do lekarza",
                    audienceDescription = "Seniorzy z małych miejscowości",
                    expectedChange = "Dojazd do lekarza bez zależności od rodziny",
                    futureVision = "Można powtórzyć w innych gminach",
                )

            val created =
                client
                    .post(Innovations()) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(withSections)
                    }.body<InnovationDto>()
            assertEquals("Nowe podejście do transportu", created.innovativeness)
            assertEquals("Starsi mieszkańcy wsi nie mają jak dojechać do lekarza", created.problemDiagnosis)
            assertEquals("Seniorzy z małych miejscowości", created.audienceDescription)
            assertEquals("Dojazd do lekarza bez zależności od rodziny", created.expectedChange)
            assertEquals("Można powtórzyć w innych gminach", created.futureVision)
            assertEquals(created, client.get(Innovations.ById(id = created.id)).body<InnovationDto>())

            val cleared =
                client
                    .put(Innovations.ById(id = created.id)) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(
                            withSections.copy(
                                innovativeness = null,
                                problemDiagnosis = "Zmieniona diagnoza",
                                futureVision = " ",
                            ),
                        )
                    }.body<InnovationDto>()
            assertNull(cleared.innovativeness)
            assertEquals("Zmieniona diagnoza", cleared.problemDiagnosis)
            assertEquals("Seniorzy z małych miejscowości", cleared.audienceDescription)
            assertNull(cleared.futureVision)
        }

    @Test
    fun innovationWithoutSectionsAndOverlongSectionsAreHandled() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val plain =
                client
                    .post(Innovations()) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(sampleRequest(title = "Bez sekcji ${System.nanoTime()}"))
                    }.body<InnovationDto>()
            assertNull(plain.problemDiagnosis)

            val tooLong =
                client.post(Innovations()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(sampleRequest().copy(audienceDescription = "x".repeat(5001)))
                }
            assertEquals(HttpStatusCode.BadRequest, tooLong.status)
            assertTrue(tooLong.body<ErrorResponse>().details.any { it.field == "audienceDescription" })
        }
}
