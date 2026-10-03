package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.realmRoles
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.GreetingService
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class MeResponse(
    val id: String?,
    val username: String?,
    val email: String?,
    val roles: Set<String>,
)

@Serializable
data class MessageResponse(val message: String)

fun Route.appRoutes() {
    val greetingService by inject<GreetingService>()

    // Public
    get("/") {
        call.respondText(greetingService.greet("Ktor"))
    }
    get("/health") {
        call.respond(HealthResponse("UP"))
    }

    // Requires a valid Keycloak access token
    authenticate(KEYCLOAK_AUTH) {
        route("/api") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respond(
                    MeResponse(
                        id = principal.subject,
                        username = principal.payload.getClaim("preferred_username").asString(),
                        email = principal.payload.getClaim("email").asString(),
                        roles = principal.realmRoles,
                    )
                )
            }

            requireRole("admin") {
                get("/admin") {
                    call.respond(MessageResponse("Hello, admin!"))
                }
            }
        }
    }
}
