package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.AdminApplications
import io.github.mfabisiak.hubmi.api.Applications
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.currentUser
import io.github.mfabisiak.hubmi.auth.requireRole
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.common.http.respondEitherUnit
import io.github.mfabisiak.hubmi.common.http.respondResult
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
    val applicationService by inject<ApplicationService>()

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
        call.respondResult(applicationService.getDeclarations(params.parent.id, params.applicantType))
    }

    authenticate(KEYCLOAK_AUTH) {
        post<Calls.ById.Applications> { params ->
            respondEither(HttpStatusCode.Created) {
                val user = call.currentUser.bind()
                val request =
                    Either
                        .catch { call.receive<CreateApplicationDraftRequest?>() }
                        .getOrNull()
                val dto = applicationService.apply(params.parent.id, user.id, request).bind()
                call.response.header(HttpHeaders.Location, "/api/applications/${dto.id}")
                dto
            }
        }

        put<Applications.ById> { params ->
            respondEither {
                val user = call.currentUser.bind()
                val request = call.receive<SaveApplicationDraftRequest>()
                applicationService
                    .saveDraft(
                        idString = params.id,
                        callerId = user.id,
                        request = request,
                    ).bind()
            }
        }

        post<Applications.ById.Submit> { params ->
            respondEither {
                val user = call.currentUser.bind()
                applicationService
                    .submit(
                        idString = params.parent.id,
                        callerId = user.id,
                    ).bind()
            }
        }

        get<Applications.Mine> { params ->
            respondEither {
                val user = call.currentUser.bind()
                applicationService
                    .listMyApplications(
                        callerId = user.id,
                        page = params.page,
                        size = params.size,
                    ).bind()
            }
        }

        get<Applications.ById> { params ->
            respondEither {
                val user = call.currentUser.bind()
                applicationService
                    .getApplicationById(
                        idString = params.id,
                        callerId = user.id,
                        isAdmin = user.roles.contains(Role.ADMIN),
                    ).bind()
            }
        }

        requireRole(Role.ADMIN) {
            post<Calls> {
                respondEither(HttpStatusCode.Created) {
                    val request = call.receive<UpsertCallRequest>()
                    val dto = grantCallService.create(request).bind()
                    call.response.header(HttpHeaders.Location, "/api/calls/${dto.id}")
                    dto
                }
            }

            put<Calls.ById> { params ->
                respondEither {
                    val request = call.receive<UpsertCallRequest>()
                    grantCallService.update(params.id, request).bind()
                }
            }

            delete<Calls.ById> { params ->
                respondEitherUnit {
                    grantCallService.delete(params.id).bind()
                }
            }

            get<AdminApplications> { params ->
                respondEither {
                    applicationService
                        .adminListApplications(
                            callIdString = params.callId,
                            status = params.status,
                            page = params.page,
                            size = params.size,
                        ).bind()
                }
            }
        }
    }
}
