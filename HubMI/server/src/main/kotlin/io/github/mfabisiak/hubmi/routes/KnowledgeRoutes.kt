package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.Challenges
import io.github.mfabisiak.hubmi.api.Materials
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.ChallengeService
import io.github.mfabisiak.hubmi.service.MaterialService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.knowledgeRoutes() {
    val challengeService by inject<ChallengeService>()
    val materialService by inject<MaterialService>()

    // Challenges - Public
    get<Challenges> { resource ->
        respondEither(challengeService.list(resource.area, resource.page, resource.size))
    }

    get<Challenges.ById> { resource ->
        respondEither(challengeService.getById(resource.id))
    }

    // Materials - Public
    get<Materials> { resource ->
        respondEither(materialService.list(resource.q, resource.area, resource.type, resource.page, resource.size))
    }

    get<Materials.ById> { resource ->
        respondEither(materialService.getById(resource.id))
    }

    // Admin only
    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
            // Challenges - Admin CRUD
            post<Challenges> {
                val request = call.receive<UpsertChallengeRequest>()
                respondEither(challengeService.create(request), successStatus = HttpStatusCode.Created)
            }

            put<Challenges.ById> { resource ->
                val request = call.receive<UpsertChallengeRequest>()
                respondEither(challengeService.update(resource.id, request))
            }

            delete<Challenges.ById> { resource ->
                respondEitherUnit(challengeService.delete(resource.id))
            }

            // Materials - Admin CRUD
            post<Materials> {
                val request = call.receive<UpsertMaterialRequest>()
                respondEither(materialService.create(request), successStatus = HttpStatusCode.Created)
            }

            put<Materials.ById> { resource ->
                val request = call.receive<UpsertMaterialRequest>()
                respondEither(materialService.update(resource.id, request))
            }

            delete<Materials.ById> { resource ->
                respondEitherUnit(materialService.delete(resource.id))
            }
        }
    }
}
