package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.CreateSampleRequest
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SampleDto
import io.github.mfabisiak.hubmi.api.Samples
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
import kotlin.test.*

class SampleRouteTest {
    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-routes",
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun uniqueSlug(prefix: String) = "$prefix-${System.nanoTime()}"

    private fun ApplicationTestBuilder.createJsonClient() =
        createClient {
            install(Resources)
            install(ContentNegotiation) { json() }
        }

    @Test
    fun getSamplesReturnsOk() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()

            val response = client.get(Samples())
            assertEquals(HttpStatusCode.OK, response.status)
            val page = response.body<Page<SampleDto>>()
            assertNotNull(page)
        }

    @Test
    fun postSampleRequiresAuthentication() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()

            val response =
                client.post(Samples()) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateSampleRequest(
                            slug = uniqueSlug("unauthenticated"),
                            name = "Unauthenticated",
                            description = "No token",
                        ),
                    )
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun postSampleValidatesInput() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val token = TestSecurityHelper.generateToken(roles = setOf("user"))

            val response =
                client.post(Samples()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateSampleRequest(
                            slug = "INVALID SLUG WITH SPACES",
                            name = "",
                            description = "",
                        ),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("validation_failed", error.code)
        }

    @Test
    fun postSampleSucceedsWithValidData() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val token = TestSecurityHelper.generateToken(roles = setOf("user"))
            val slug = uniqueSlug("valid-sample")

            val response =
                client.post(Samples()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateSampleRequest(
                            slug = slug,
                            name = "Valid Sample",
                            description = "Valid Description",
                        ),
                    )
                }

            assertEquals(HttpStatusCode.Created, response.status)
            val created = response.body<SampleDto>()
            assertEquals(slug, created.slug)
            assertEquals("Valid Sample", created.name)
            assertNotNull(created.id)
        }

    @Test
    fun postSampleWithDuplicateSlugReturnsConflict() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val token = TestSecurityHelper.generateToken(roles = setOf("user"))
            val slug = uniqueSlug("conflict-slug")

            // First creation
            val first =
                client.post(Samples()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateSampleRequest(
                            slug = slug,
                            name = "First Item",
                            description = "First Desc",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Created, first.status)

            // Second creation with same slug
            val second =
                client.post(Samples()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateSampleRequest(
                            slug = slug,
                            name = "Second Item",
                            description = "Second Desc",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Conflict, second.status)
            val error = second.body<ErrorResponse>()
            assertEquals("conflict", error.code)
        }

    @Test
    fun deleteSampleRequiresAdminRole() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()

            // 1. Create a sample as user
            val userToken = TestSecurityHelper.generateToken(roles = setOf("user"))
            val slug = uniqueSlug("to-delete")
            val createResponse =
                client.post(Samples()) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateSampleRequest(
                            slug = slug,
                            name = "Delete Test",
                            description = "Desc",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<SampleDto>()

            // 2. Try to delete with regular user token -> 403 Forbidden
            val forbiddenResponse =
                client.delete(Samples.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)

            // 3. Delete with admin token -> 204 No Content
            val adminToken = TestSecurityHelper.generateToken(roles = setOf("admin"))
            val successResponse =
                client.delete(Samples.ById(id = created.id)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.NoContent, successResponse.status)
        }
}
