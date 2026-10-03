package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.AdminApplications
import io.github.mfabisiak.hubmi.api.Applications
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
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

    get<Calls.ById.Declarations> { params ->
        call.respondResult(grantCallService.getDeclarations(params.parent.id, params.applicantType))
    }

    authenticate(KEYCLOAK_AUTH) {
        post<Calls.ById.Applications> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    val request = call.receive<CreateApplicationDraftRequest>()
                    val result = grantCallService.apply(params.parent.id, user.id, request)
                    result.fold(
                        ifLeft = { call.respondError(it) },
                        ifRight = { dto ->
                            call.response.header(
                                HttpHeaders.Location,
                                "/api/applications/${dto.id}",
                            )
                            call.respond(HttpStatusCode.Created, dto)
                        },
                    )
                },
            )
        }

        put<Applications.ById> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    val request = call.receive<SaveApplicationDraftRequest>()
                    call.respondResult(
                        grantCallService.saveDraft(
                            idString = params.id,
                            callerId = user.id,
                            isAdmin = user.roles.contains(Role.ADMIN),
                            request = request,
                        ),
                    )
                },
            )
        }

        post<Applications.ById.Submit> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    call.respondResult(
                        grantCallService.submit(
                            idString = params.parent.id,
                            callerId = user.id,
                            isAdmin = user.roles.contains(Role.ADMIN),
                        ),
                    )
                },
            )
        }

        get<Applications.Mine> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    call.respondResult(
                        grantCallService.listMyApplications(
                            callerId = user.id,
                            page = params.page,
                            size = params.size,
                        ),
                    )
                },
            )
        }

        get<Applications.ById> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    call.respondResult(
                        grantCallService.getApplicationById(
                            idString = params.id,
                            callerId = user.id,
                            isAdmin = user.roles.contains(Role.ADMIN),
                        ),
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

            get<AdminApplications> { params ->
                call.respondResult(
                    grantCallService.adminListApplications(
                        callIdString = params.callId,
                        status = params.status,
                        page = params.page,
                        size = params.size,
                    ),
                )
            }
        }
    }
}
