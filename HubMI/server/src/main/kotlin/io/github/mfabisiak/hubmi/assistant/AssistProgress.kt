package io.github.mfabisiak.hubmi.assistant

import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.AssistEvent
import io.github.mfabisiak.hubmi.api.AssistMode
import io.github.mfabisiak.hubmi.api.AssistResponse
import io.github.mfabisiak.hubmi.api.FlowDto
import io.github.mfabisiak.hubmi.api.NoveltyHint
import io.github.mfabisiak.hubmi.api.SimilarInnovationDto
import io.github.mfabisiak.hubmi.api.SuggestionDto

/** What the model has produced so far, which is all that the closing [AssistEvent.Done] needs. */
internal data class AssistProgress(
    val suggestions: List<SuggestionDto>,
    val flow: FlowDto?,
    val ended: AiStatus?,
) {
    fun after(step: Generated): AssistProgress =
        when (step) {
            is Generated.Ended -> copy(ended = step.status)
            is Generated.Produced -> afterProduced(step.event)
        }

    private fun afterProduced(event: AssistEvent): AssistProgress =
        when (event) {
            is AssistEvent.Suggestion -> copy(suggestions = suggestions + event.suggestion)
            is AssistEvent.Flow -> copy(flow = event.flow)
            is AssistEvent.Similar, is AssistEvent.Done -> this
        }

    /** What was produced counts even if the deadline cut the model short, which leaves [ended] unset. */
    private fun status(): AiStatus =
        when {
            suggestions.isNotEmpty() || flow != null -> AiStatus.OK
            ended == null -> AiStatus.UNAVAILABLE
            ended == AiStatus.OK -> AiStatus.INVALID_OUTPUT
            else -> ended
        }

    fun response(
        mode: AssistMode,
        similar: List<SimilarInnovationDto>,
        novelty: NoveltyHint,
    ): AssistResponse =
        AssistResponse(
            mode = mode,
            aiStatus = status(),
            similar = similar,
            noveltyHint = novelty,
            suggestions = suggestions,
            flow = flow,
        )

    companion object {
        val START = AssistProgress(suggestions = emptyList(), flow = null, ended = null)
    }
}
