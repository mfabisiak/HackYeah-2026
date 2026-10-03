package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.Challenges
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.Materials
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
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

class KnowledgeRouteTest {
    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-knowledge",
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

    @Test
    fun challengeCrudFlow() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val userToken = TestSecurityHelper.generateToken(roles = setOf(Role.USER))

            // 1. List challenges -> 200
            val listResponse = client.get(Challenges())
            assertEquals(HttpStatusCode.OK, listResponse.status)
            val initialPage = listResponse.body<Page<ChallengeDto>>()
            assertNotNull(initialPage)

            // 2. Create without token -> 401
            val challengeRequest =
                UpsertChallengeRequest(
                    title = "Wyzwanie testowe ${System.nanoTime()}",
                    description = "Opis wyzwania społecznego",
                    area = SocialArea.LONELINESS,
                    municipalities = listOf("Kraków", "Wieliczka"),
                )
            val unauthResponse =
                client.post(Challenges()) {
                    contentType(ContentType.Application.Json)
                    setBody(challengeRequest)
                }
            assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

            // 3. Create with user role -> 403
            val forbiddenResponse =
                client.post(Challenges()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(userToken)
                    setBody(challengeRequest)
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)

            // 4. Create with admin role -> 201
            val createResponse =
                client.post(Challenges()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(challengeRequest)
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<ChallengeDto>()
            assertEquals(challengeRequest.title, created.title)

            // 5. Get by ID -> 200
            val getResponse = client.get(Challenges.ById(id = created.id))
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val fetched = getResponse.body<ChallengeDto>()
            assertEquals(created.id, fetched.id)

            // 6. Update -> 200
            val updatedRequest = challengeRequest.copy(title = "Zaktualizowane wyzwanie ${System.nanoTime()}")
            val putResponse =
                client.put(Challenges.ById(id = created.id)) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(updatedRequest)
                }
            assertEquals(HttpStatusCode.OK, putResponse.status)
            val updated = putResponse.body<ChallengeDto>()
            assertEquals(updatedRequest.title, updated.title)

            // 7. Delete -> 204
            val deleteResponse =
                client.delete(Challenges.ById(id = created.id)) {
                    bearerAuth(adminToken)
                }
            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // 8. Subsequent GET -> 404
            val afterDeleteResponse = client.get(Challenges.ById(id = created.id))
            assertEquals(HttpStatusCode.NotFound, afterDeleteResponse.status)
        }

    @Test
    fun materialCrudFlow() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val userToken = TestSecurityHelper.generateToken(roles = setOf(Role.USER))

            // 1. List materials -> 200
            val listResponse = client.get(Materials())
            assertEquals(HttpStatusCode.OK, listResponse.status)
            val initialPage = listResponse.body<Page<MaterialDto>>()
            assertNotNull(initialPage)

            // 2. Create without token -> 401
            val materialRequest =
                UpsertMaterialRequest(
                    title = "Poradnik A11y ${System.nanoTime()}",
                    description = "Jak pisać kod dostępny wg WCAG 2.1 AA",
                    type = MaterialType.GUIDE,
                    url = "https://rops.krakow.pl/poradnik-a11y.pdf",
                    areas = listOf(SocialArea.DIGITAL_EXCLUSION),
                )
            val unauthResponse =
                client.post(Materials()) {
                    contentType(ContentType.Application.Json)
                    setBody(materialRequest)
                }
            assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

            // 3. Create with user role -> 403
            val forbiddenResponse =
                client.post(Materials()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(userToken)
                    setBody(materialRequest)
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)

            // 4. Create with admin role -> 201
            val createResponse =
                client.post(Materials()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(materialRequest)
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<MaterialDto>()
            assertEquals(materialRequest.title, created.title)

            // 5. Get by ID -> 200
            val getResponse = client.get(Materials.ById(id = created.id))
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val fetched = getResponse.body<MaterialDto>()
            assertEquals(created.id, fetched.id)

            // 6. Update -> 200
            val updatedRequest = materialRequest.copy(title = "Zaktualizowany Poradnik ${System.nanoTime()}")
            val putResponse =
                client.put(Materials.ById(id = created.id)) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(updatedRequest)
                }
            assertEquals(HttpStatusCode.OK, putResponse.status)
            val updated = putResponse.body<MaterialDto>()
            assertEquals(updatedRequest.title, updated.title)

            // 7. Delete -> 204
            val deleteResponse =
                client.delete(Materials.ById(id = created.id)) {
                    bearerAuth(adminToken)
                }
            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // 8. Subsequent GET -> 404
            val afterDeleteResponse = client.get(Materials.ById(id = created.id))
            assertEquals(HttpStatusCode.NotFound, afterDeleteResponse.status)
        }

    @Test
    fun challengeValidationAndInvalidIdYieldBadRequest() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val response =
                client.post(Challenges()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(
                        UpsertChallengeRequest(
                            title = " ",
                            description = "Opis",
                            area = SocialArea.LONELINESS,
                            municipalities = listOf("Kraków", ""),
                        ),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val fields = response.body<ErrorResponse>().details.map { it.field }
            assertEquals(setOf("title", "municipalities[1]"), fields.toSet())
            assertEquals(HttpStatusCode.BadRequest, client.get(Challenges.ById(id = "xyz")).status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/api/challenges?area=NIE_ISTNIEJE").status)
        }

    @Test
    fun materialValidationAndFilters() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

            val invalid =
                client.post(Materials()) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(adminToken)
                    setBody(
                        UpsertMaterialRequest(
                            title = "Poradnik",
                            description = "Opis",
                            type = MaterialType.GUIDE,
                            url = "ftp://example.com",
                            areas = emptyList(),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, invalid.status)
            assertEquals(
                setOf("url", "areas"),
                invalid
                    .body<ErrorResponse>()
                    .details
                    .map { it.field }
                    .toSet(),
            )

            val marker = "Znacznik${System.nanoTime()}"
            val created =
                client
                    .post(Materials()) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(
                            UpsertMaterialRequest(
                                title = "  $marker.*  ",
                                description = "Opis materiału",
                                type = MaterialType.VIDEO,
                                url = " https://example.com/film ",
                                areas = listOf(SocialArea.DIGITAL_EXCLUSION),
                            ),
                        )
                    }.body<MaterialDto>()
            assertEquals("https://example.com/film", created.url)

            // `q` is a literal substring (regex metacharacters are not interpreted), case-insensitive
            val found = client.get(Materials(q = marker.lowercase() + ".*")).body<Page<MaterialDto>>()
            assertEquals(listOf(created.id), found.items.map { it.id })
            val notFound = client.get(Materials(q = marker.lowercase() + "x", area = SocialArea.DIGITAL_EXCLUSION))
            assertEquals(0, notFound.body<Page<MaterialDto>>().total)
            val byType = client.get(Materials(q = marker, type = MaterialType.REPORT)).body<Page<MaterialDto>>()
            assertEquals(0, byType.total)
            assertEquals(HttpStatusCode.BadRequest, client.get(Materials(q = "x".repeat(101))).status)
        }
}
