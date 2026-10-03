package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.CreateSampleRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.Samples
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.SampleService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.sampleRoutes() {
    val service by inject<SampleService>()

    // Public
    get<Samples> { resource ->
        respondEither(service.list(resource.page, resource.size))
    }

    get<Samples.ById> { resource ->
        respondEither(service.getById(resource.id))
    }

    // Authenticated
    authenticate(KEYCLOAK_AUTH) {
        post<Samples> {
            val request = call.receive<CreateSampleRequest>()
            respondEither(service.create(request), successStatus = HttpStatusCode.Created)
        }

        // Admin only
        requireRole(Role.ADMIN) {
            delete<Samples.ById> { resource ->
                respondEitherUnit(service.delete(resource.id))
            }
        }
    }
}
