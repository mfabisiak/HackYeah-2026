package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.Api
import io.github.mfabisiak.hubmi.api.Health
import io.github.mfabisiak.hubmi.api.HealthResponse
import io.github.mfabisiak.hubmi.api.MessageResponse
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.models.toMeResponse
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.repository.MongoRepository
import io.github.mfabisiak.hubmi.service.GreetingService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.appRoutes() {
    val greetingService by inject<GreetingService>()
    val mongo by inject<MongoRepository>()

    // Public
    get("/") {
        call.respondText(greetingService.greet("Ktor"))
    }
    get<Health> {
        call.respond(HealthResponse("UP"))
    }
    get<Health.Ready> {
        mongo.ping().fold(
            ifLeft = { call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse("DOWN")) },
            ifRight = { call.respond(HealthResponse("UP")) },
        )
    }

    ideaRoutes()
    grantCallRoutes()
    contractStubs()

    // Requires a valid Keycloak access token
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
