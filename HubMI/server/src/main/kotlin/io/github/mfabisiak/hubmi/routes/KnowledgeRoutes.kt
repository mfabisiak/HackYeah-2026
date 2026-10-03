package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.Challenges
import io.github.mfabisiak.hubmi.api.Materials
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.domain.ChallengeDraft
import io.github.mfabisiak.hubmi.domain.ChallengeId
import io.github.mfabisiak.hubmi.domain.MaterialDraft
import io.github.mfabisiak.hubmi.domain.MaterialId
import io.github.mfabisiak.hubmi.domain.SearchQuery
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.github.mfabisiak.hubmi.service.ChallengeService
import io.github.mfabisiak.hubmi.service.MaterialService
import io.github.mfabisiak.hubmi.service.orValidationError
import io.github.mfabisiak.hubmi.service.validatePageRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

private const val INVALID_CHALLENGE_ID = "Nieprawidłowy identyfikator wyzwania"
private const val INVALID_CHALLENGE = "Błąd walidacji danych wyzwania"
private const val INVALID_MATERIAL_ID = "Nieprawidłowy identyfikator materiału"
private const val INVALID_MATERIAL = "Błąd walidacji danych materiału"

fun Route.knowledgeRoutes() {
    val challengeService by inject<ChallengeService>()
    val materialService by inject<MaterialService>()

    // Challenges - Public
    get<Challenges> { resource ->
        respondEither {
            val pageRequest = validatePageRequest(resource.page, resource.size).bind()
            challengeService.list(resource.area, pageRequest).bind()
        }
    }

    get<Challenges.ById> { resource ->
        respondEither {
            val id = ChallengeId.parse(resource.id).orValidationError(INVALID_CHALLENGE_ID).bind()
            challengeService.getById(id).bind()
        }
    }

    // Materials - Public
    get<Materials> { resource ->
        respondEither {
            val pageRequest = validatePageRequest(resource.page, resource.size).bind()
            val q = SearchQuery.parse(resource.q).orValidationError("Nieprawidłowe zapytanie").bind()
            materialService.list(q, resource.area, resource.type, pageRequest).bind()
        }
    }

    get<Materials.ById> { resource ->
        respondEither {
            val id = MaterialId.parse(resource.id).orValidationError(INVALID_MATERIAL_ID).bind()
            materialService.getById(id).bind()
        }
    }

    // Admin only
    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
            // Challenges - Admin CRUD
            post<Challenges> {
                val request = call.receive<UpsertChallengeRequest>()
                respondEither(successStatus = HttpStatusCode.Created) {
                    val draft = ChallengeDraft.parse(request).orValidationError(INVALID_CHALLENGE).bind()
                    challengeService.create(draft).bind()
                }
            }

            put<Challenges.ById> { resource ->
                val request = call.receive<UpsertChallengeRequest>()
                respondEither {
                    val id = ChallengeId.parse(resource.id).orValidationError(INVALID_CHALLENGE_ID).bind()
                    val draft = ChallengeDraft.parse(request).orValidationError(INVALID_CHALLENGE).bind()
                    challengeService.update(id, draft).bind()
                }
            }

            delete<Challenges.ById> { resource ->
                respondEitherUnit {
                    val id = ChallengeId.parse(resource.id).orValidationError(INVALID_CHALLENGE_ID).bind()
                    challengeService.delete(id).bind()
                }
            }

            // Materials - Admin CRUD
            post<Materials> {
                val request = call.receive<UpsertMaterialRequest>()
                respondEither(successStatus = HttpStatusCode.Created) {
                    val draft = MaterialDraft.parse(request).orValidationError(INVALID_MATERIAL).bind()
                    materialService.create(draft).bind()
                }
            }

            put<Materials.ById> { resource ->
                val request = call.receive<UpsertMaterialRequest>()
                respondEither {
                    val id = MaterialId.parse(resource.id).orValidationError(INVALID_MATERIAL_ID).bind()
                    val draft = MaterialDraft.parse(request).orValidationError(INVALID_MATERIAL).bind()
                    materialService.update(id, draft).bind()
                }
            }

            delete<Materials.ById> { resource ->
                respondEitherUnit {
                    val id = MaterialId.parse(resource.id).orValidationError(INVALID_MATERIAL_ID).bind()
                    materialService.delete(id).bind()
                }
            }
        }
    }
}
