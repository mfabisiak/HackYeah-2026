package io.github.mfabisiak.hubmi.auth

import io.github.mfabisiak.hubmi.api.Api
import io.github.mfabisiak.hubmi.api.MessageResponse
import io.github.mfabisiak.hubmi.api.Role
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Endpoints that need a valid Keycloak access token. */
fun Route.authRoutes() {
    authenticate(KEYCLOAK_AUTH) {
        get<Api.Me> {
            call
                .principal<JWTPrincipal>()
                ?.let { call.respond(it.toMeResponse()) }
                ?: call.respond(HttpStatusCode.Unauthorized)
        }

        requireRole(Role.ADMIN) {
            get<Api.Admin> {
                call.respond(MessageResponse("Hello, admin!"))
            }
        }
    }
}
