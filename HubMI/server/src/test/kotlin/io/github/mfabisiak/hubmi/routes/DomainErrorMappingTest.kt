package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.plugins.configureSerialization
import io.github.mfabisiak.hubmi.service.DomainError
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.*

class DomainErrorMappingTest {
    @Test
    fun testAllDomainErrorsMapToCorrectHttpStatusAndErrorResponse() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    get("/test-not-found") {
                        respondError(DomainError.NotFound("Not found test"))
                    }
                    get("/test-conflict") {
                        respondError(DomainError.Conflict("Conflict test"))
                    }
                    get("/test-validation") {
                        respondError(
                            DomainError.Validation(
                                message = "Validation test",
                                details = listOf(FieldError("name", "required", "Pole wymagane")),
                            ),
                        )
                    }
                    get("/test-unauthorized") {
                        respondError(DomainError.Unauthorized("Unauthorized test"))
                    }
                    get("/test-forbidden") {
                        respondError(DomainError.Forbidden("Forbidden test"))
                    }
                    get("/test-unavailable") {
                        respondError(DomainError.Unavailable("Unavailable test"))
                    }
                    get("/test-internal") {
                        respondError(DomainError.Internal("Internal test"))
                    }
                }
            }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // NotFound -> 404
            val notFoundRes = client.get("/test-not-found")
            assertEquals(HttpStatusCode.NotFound, notFoundRes.status)
            assertEquals("not_found", notFoundRes.body<ErrorResponse>().code)

            // Conflict -> 409
            val conflictRes = client.get("/test-conflict")
            assertEquals(HttpStatusCode.Conflict, conflictRes.status)
            assertEquals("conflict", conflictRes.body<ErrorResponse>().code)

            // Validation -> 400 + details
            val validationRes = client.get("/test-validation")
            assertEquals(HttpStatusCode.BadRequest, validationRes.status)
            val valBody = validationRes.body<ErrorResponse>()
            assertEquals("validation_failed", valBody.code)
            assertEquals(1, valBody.details.size)
            assertEquals("name", valBody.details.first().field)

            // Unauthorized -> 401
            val unauthRes = client.get("/test-unauthorized")
            assertEquals(HttpStatusCode.Unauthorized, unauthRes.status)
            assertEquals("unauthorized", unauthRes.body<ErrorResponse>().code)

            // Forbidden -> 403
            val forbiddenRes = client.get("/test-forbidden")
            assertEquals(HttpStatusCode.Forbidden, forbiddenRes.status)
            assertEquals("forbidden", forbiddenRes.body<ErrorResponse>().code)

            // Unavailable -> 503
            val unavailRes = client.get("/test-unavailable")
            assertEquals(HttpStatusCode.ServiceUnavailable, unavailRes.status)
            assertEquals("service_unavailable", unavailRes.body<ErrorResponse>().code)

            // Internal -> 500
            val internalRes = client.get("/test-internal")
            assertEquals(HttpStatusCode.InternalServerError, internalRes.status)
            assertEquals("internal_error", internalRes.body<ErrorResponse>().code)
        }
}
