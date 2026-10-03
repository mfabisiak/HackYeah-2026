package io.github.mfabisiak.hubmi.ideas

import io.github.mfabisiak.hubmi.api.AdminIdeas
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.currentUser
import io.github.mfabisiak.hubmi.auth.requireRole
import io.github.mfabisiak.hubmi.common.http.respondEither
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
            respondEither(HttpStatusCode.Created) {
                val user = call.currentUser.bind()
                val request = call.receive<CreateIdeaRequest>()
                val dto = ideaService.create(user.id, request).bind()
                call.response.header(HttpHeaders.Location, "/api/ideas/${dto.id}")
                dto
            }
        }

        get<Ideas.Mine> { params ->
            respondEither {
                val user = call.currentUser.bind()
                ideaService.getMine(user.id, params.page, params.size).bind()
            }
        }

        get<Ideas.ById> { params ->
            respondEither {
                val user = call.currentUser.bind()
                ideaService.getById(params.id, user.id, user.roles).bind()
            }
        }

        requireRole(Role.ADMIN) {
            get<AdminIdeas> { params ->
                respondEither {
                    ideaService.listForAdmin(params.status, params.page, params.size).bind()
                }
            }

            patch<AdminIdeas.Status> { params ->
                respondEither {
                    val request = call.receive<UpdateIdeaStatusRequest>()
                    ideaService.updateStatus(params.id, request).bind()
                }
            }
        }
    }
}
