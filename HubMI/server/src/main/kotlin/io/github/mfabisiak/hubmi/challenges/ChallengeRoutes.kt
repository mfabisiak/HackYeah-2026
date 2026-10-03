package io.github.mfabisiak.hubmi.challenges

import io.github.mfabisiak.hubmi.api.Challenges
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.requireRole
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.common.http.respondEitherUnit
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.validatePageRequest
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

fun Route.challengeRoutes() {
    val challengeService by inject<ChallengeService>()

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

    // Admin only
    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
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
        }
    }
}
