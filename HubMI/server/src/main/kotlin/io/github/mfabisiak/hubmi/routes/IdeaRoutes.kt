package io.github.mfabisiak.hubmi.routes

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.AdminIdeas
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.currentUser
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.IdeaService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.ideaRoutes() {
    val ideaService by inject<IdeaService>()

    authenticate(KEYCLOAK_AUTH) {
        post<Ideas> {
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    val request = call.receive<CreateIdeaRequest>()
                    val result = ideaService.create(user.id, request)
                    result.fold(
                        ifLeft = { call.respondError(it) },
                        ifRight = { dto ->
                            call.response.header(HttpHeaders.Location, "/api/ideas/${dto.id}")
                            call.respond(HttpStatusCode.Created, dto)
                        },
                    )
                },
            )
        }

        get<Ideas.Mine> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    call.respondResult(ideaService.getMine(user.id, params.page, params.size))
                },
            )
        }

        get<Ideas.ById> { params ->
            val userOrError = call.currentUser
            userOrError.fold(
                ifLeft = { call.respondError(it) },
                ifRight = { user ->
                    call.respondResult(ideaService.getById(params.id, user.id, user.roles))
                },
            )
        }

        requireRole(Role.ADMIN) {
            get<AdminIdeas> { params ->
                call.respondResult(ideaService.adminList(params.status, params.page, params.size))
            }

            patch<AdminIdeas.Status> { params ->
                val request = call.receive<UpdateIdeaStatusRequest>()
                call.respondResult(ideaService.adminUpdateStatus(params.id, request))
            }
        }
    }
}
