package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.AssistDraftRequest
import io.github.mfabisiak.hubmi.api.AssistEvent
import io.github.mfabisiak.hubmi.api.AssistRequest
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.currentUser
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.http.respondError
import io.github.mfabisiak.hubmi.common.http.respondEventStream
import io.github.mfabisiak.hubmi.common.http.wantsEventStream
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.ideas.IdeaDraft
import io.github.mfabisiak.hubmi.ideas.IdeaService
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.koin.ktor.ext.inject

private const val INVALID_IDEA = "Błąd walidacji pomysłu"

/**
 * The assistant answers in one of two ways on the same routes: as JSON when the client asks for it, or as a stream of
 * server-sent events (`Accept: text/event-stream`) so that the first suggestion is on screen long before the last
 * is written. Errors that are known before any generation (login, validation, access, the library lookup) are ordinary
 * error responses in both cases; the model failing is not an error but the `aiStatus` of the answer.
 */
fun Route.assistRoutes() {
    val service by inject<AssistService>()
    val ideas by inject<IdeaService>()

    authenticate(KEYCLOAK_AUTH) {
        post<Ideas.Assist> {
            val request = call.receive<AssistDraftRequest>()
            respondAssist(
                either {
                    call.currentUser.bind()
                    val draft = IdeaDraft.parse(request.idea).orValidationError(INVALID_IDEA).bind()
                    service.assist(AssistIdea.of(draft), request.mode).bind()
                },
            )
        }

        post<Ideas.ById.Assist> { resource ->
            val request = call.receive<AssistRequest>()
            respondAssist(
                either {
                    val user = call.currentUser.bind()
                    val idea = ideas.getById(resource.parent.id, user.id, user.roles).bind()
                    service.assist(AssistIdea.of(idea), request.mode).bind()
                },
            )
        }
    }
}

private suspend fun RoutingContext.respondAssist(answer: Either<DomainError, Flow<AssistEvent>>) =
    answer.fold(
        ifLeft = { respondError(it) },
        ifRight = { events ->
            if (call.wantsEventStream()) {
                respondEventStream(events, AssistEvent.serializer(), AssistEvent::eventName)
            } else {
                call.respond(events.filterIsInstance<AssistEvent.Done>().first().response)
            }
        },
    )

private fun AssistEvent.eventName(): String =
    when (this) {
        is AssistEvent.Similar -> AssistEvent.SIMILAR
        is AssistEvent.Suggestion -> AssistEvent.SUGGESTION
        is AssistEvent.Flow -> AssistEvent.FLOW
        is AssistEvent.Done -> AssistEvent.DONE
    }
