package io.github.mfabisiak.hubmi.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
)

fun Application.configureSerialization() {
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log
                .error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
        }
        status(HttpStatusCode.Forbidden) { call, _ ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("Forbidden"))
        }
    }
}
