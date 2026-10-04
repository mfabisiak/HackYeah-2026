package io.github.mfabisiak.hubmi.web.assistant

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.mfabisiak.hubmi.api.AssistDraftRequest
import io.github.mfabisiak.hubmi.api.AssistEvent
import io.github.mfabisiak.hubmi.api.AssistMode
import io.github.mfabisiak.hubmi.api.AssistRequest
import io.github.mfabisiak.hubmi.api.AssistResponse
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.SseMessage
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.StreamJs
import io.github.mfabisiak.hubmi.web.callSafely
import io.github.mfabisiak.hubmi.web.collectEvents
import io.github.mfabisiak.hubmi.web.decoder
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.eventStream
import io.github.mfabisiak.hubmi.web.ideas.CreateIdeaJs
import io.github.mfabisiak.hubmi.web.ideas.toDto
import io.github.mfabisiak.hubmi.web.startStream
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * The idea creator's assistant (`/api/ideas/assist`, `/api/ideas/{id}/assist`); all calls need login.
 *
 * Every call returns a [StreamJs] at once. Its `result` promise resolves to the whole answer, so a caller that only
 * wants that ignores the listener; one that wants to show suggestions as they are written passes an
 * [AssistListenerJs] with `onSimilar`, `onSuggestion` and `onFlow`. The model's part can fail without an error: check
 * `aiStatus` of the answer.
 */
@JsExport
class AssistantApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    /** For an idea that is not saved yet, as typed in the form. @param mode an `AssistMode` name. */
    fun assistDraft(
        idea: CreateIdeaJs,
        mode: String,
        listener: AssistListenerJs? = null,
    ): StreamJs<AssistResponseJs> =
        scope.startStream(
            call = {
                either {
                    val request = AssistDraftRequest(enumOf<AssistMode>(mode, "mode"), idea.toDto().bind())
                    answer(client.eventStream(Ideas.Assist(), request), listener).bind()
                }
            },
            transform = AssistResponse::toJs,
        )

    /** For a saved idea of the caller. @param mode an `AssistMode` name. */
    fun assistIdea(
        ideaId: String,
        mode: String,
        listener: AssistListenerJs? = null,
    ): StreamJs<AssistResponseJs> =
        scope.startStream(
            call = {
                either {
                    val request = AssistRequest(enumOf<AssistMode>(mode, "mode"))
                    val resource = Ideas.ById.Assist(Ideas.ById(id = ideaId))
                    answer(client.eventStream(resource, request), listener).bind()
                }
            },
            transform = AssistResponse::toJs,
        )

    private suspend fun answer(
        messages: Flow<Either<ApiErrorJs, SseMessage>>,
        listener: AssistListenerJs?,
    ): Either<ApiErrorJs, AssistResponse> =
        messages.collectEvents(
            decode = AssistEvent.serializer().decoder(),
            onEvent = { event -> listener?.deliver(event) },
            finish = { event -> if (event is AssistEvent.Done) event.response.right() else null },
        )
}

private fun AssistListenerJs.deliver(event: AssistEvent) {
    when (event) {
        is AssistEvent.Similar -> onSimilar?.let { callback -> callSafely { callback(event.toJs()) } }
        is AssistEvent.Suggestion -> onSuggestion?.let { callback -> callSafely { callback(event.suggestion.toJs()) } }
        is AssistEvent.Flow -> onFlow?.let { callback -> callSafely { callback(event.flow.toJs()) } }
        is AssistEvent.Done -> Unit
    }
}
