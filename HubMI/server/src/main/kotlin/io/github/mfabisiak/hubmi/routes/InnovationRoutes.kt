package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.InnovationService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.innovationRoutes() {
    val service by inject<InnovationService>()

    // Public
    get<Innovations> { resource ->
        respondEither(service.list(resource.q, resource.area, resource.targetGroup, resource.page, resource.size))
    }

    get<Innovations.ById> { resource ->
        respondEither(service.getById(resource.id))
    }

    // Admin only
    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
            post<Innovations> {
                val request = call.receive<UpsertInnovationRequest>()
                respondEither(service.create(request), successStatus = HttpStatusCode.Created)
            }

            put<Innovations.ById> { resource ->
                val request = call.receive<UpsertInnovationRequest>()
                respondEither(service.update(resource.id, request))
            }

            delete<Innovations.ById> { resource ->
                respondEitherUnit(service.delete(resource.id))
            }
        }
    }
}
