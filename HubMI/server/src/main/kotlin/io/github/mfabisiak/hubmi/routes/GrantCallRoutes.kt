package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.CreateApplicationRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.currentUser
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.GrantCallService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.grantCallRoutes() {
    val grantCallService by inject<GrantCallService>()

    get<Calls> { params ->
        call.respondResult(grantCallService.list(params.status))
    }

    get<Calls.Active> {
        call.respondResult(grantCallService.active())
    }

    get<Calls.ById> { params ->
        call.respondResult(grantCallService.getById(params.id))
    }

    authenticate(KEYCLOAK_AUTH) {
        post<Calls.ById.Applications> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    val request = call.receive<CreateApplicationRequest>()
                    val result = grantCallService.apply(params.parent.id, user.id, request)
                    result.fold(
                        ifLeft = { call.respondError(it) },
                        ifRight = { dto ->
                            call.response.header(
                                HttpHeaders.Location,
                                "/api/calls/${params.parent.id}/applications/${dto.id}",
                            )
                            call.respond(HttpStatusCode.Created, dto)
                        },
                    )
                },
            )
        }

        requireRole(Role.ADMIN) {
            post<Calls> {
                val request = call.receive<UpsertCallRequest>()
                val result = grantCallService.create(request)
                result.fold(
                    ifLeft = { call.respondError(it) },
                    ifRight = { dto ->
                        call.response.header(HttpHeaders.Location, "/api/calls/${dto.id}")
                        call.respond(HttpStatusCode.Created, dto)
                    },
                )
            }

            put<Calls.ById> { params ->
                val request = call.receive<UpsertCallRequest>()
                call.respondResult(grantCallService.update(params.id, request))
            }

            delete<Calls.ById> { params ->
                call.respondEitherUnit(grantCallService.delete(params.id))
            }
        }
    }
}
