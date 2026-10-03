package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.MatchFeedbackRequest
import io.github.mfabisiak.hubmi.api.MatchRequest
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.domain.MatchRequestDraft
import io.github.mfabisiak.hubmi.domain.NeedId
import io.github.mfabisiak.hubmi.domain.UserId
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.currentUser
import io.github.mfabisiak.hubmi.service.MatchService
import io.github.mfabisiak.hubmi.service.orValidationError
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

private const val INVALID_MATCH_REQUEST = "Błąd walidacji opisu problemu"
private const val INVALID_NEED_ID = "Nieprawidłowy identyfikator zgłoszenia"

/**
 * Open to everyone, login optional: an anonymous request works as well, but a logged-in reporter owns the need, which
 * is what later lets the platform notify them about it and invite them to tests.
 */
fun Route.matchRoutes() {
    val service by inject<MatchService>()

    authenticate(KEYCLOAK_AUTH, optional = true) {
        post<Matches> {
            val request = call.receive<MatchRequest>()
            respondEither {
                val draft = MatchRequestDraft.parse(request).orValidationError(INVALID_MATCH_REQUEST).bind()
                service.match(draft, reporter = call.currentUser.getOrNull()?.let { UserId(it.id) }).bind()
            }
        }

        put<Matches.Feedback> { resource ->
            val request = call.receive<MatchFeedbackRequest>()
            respondEitherUnit {
                val needId = NeedId.parse(resource.needId).orValidationError(INVALID_NEED_ID).bind()
                val caller = call.currentUser.getOrNull()?.let { UserId(it.id) }
                service.recordFeedback(needId, request.helpful, caller).bind()
            }
        }
    }
}
