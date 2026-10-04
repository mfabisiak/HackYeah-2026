package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.AssistEvent
import io.github.mfabisiak.hubmi.api.AssistMode
import io.github.mfabisiak.hubmi.api.NoveltyHint
import io.github.mfabisiak.hubmi.api.SimilarInnovationDto
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.toSummary
import io.github.mfabisiak.hubmi.matching.MatchingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The assistant of the idea creator. What can be known without a model is: the library's innovations closest to the
 * idea and whether it is covered already; the model only adds suggestions or a flow on top, and everything it says is
 * validated by [AssistParser]. If the model is off, slow, busy or incoherent the answer still has the deterministic
 * part and says so in [AiStatus], so the caller never gets an error for that.
 */
class AssistService(
    private val matching: MatchingEngine,
    private val llm: LlmClient,
    private val slots: LlmSlots,
    private val settings: Settings,
) {
    /** [deadline] bounds one generation including the wait for a free place at the model. */
    data class Settings(
        val enabled: Boolean,
        val deadline: Duration = DEFAULT_DEADLINE,
    )

    /**
     * Looks the idea up in the library (the only step that can fail with an error) and returns the answer as events:
     * [AssistEvent.Similar] first, then what the model produces as it produces it, and [AssistEvent.Done] with the whole.
     */
    suspend fun assist(
        idea: AssistIdea,
        mode: AssistMode,
    ): Either<DomainError, Flow<AssistEvent>> =
        either {
            val found = matching.match(idea.query(), municipality = null).bind().matches
            val similar = found.map { SimilarInnovationDto(innovation = it.innovation.toSummary(), score = it.score) }
            val novelty = noveltyOf(found.firstOrNull()?.score)
            flow {
                emit(AssistEvent.Similar(similar, novelty))
                val progress =
                    generate(idea, mode, found.map { it.innovation }).fold(AssistProgress.START) { progress, step ->
                        if (step is Generated.Produced) emit(step.event)
                        progress.after(step)
                    }
                emit(AssistEvent.Done(progress.response(mode, similar, novelty)))
            }
        }

    suspend fun warmUp(): Either<LlmError, Unit> = if (settings.enabled) llm.warmUp() else Unit.right()

    private fun generate(
        idea: AssistIdea,
        mode: AssistMode,
        candidates: List<InnovationItem>,
    ): Flow<Generated> =
        when {
            mode == AssistMode.SIMILAR -> {
                flowOf(Generated.Ended(AiStatus.NOT_REQUESTED))
            }

            !settings.enabled -> {
                flowOf(Generated.Ended(AiStatus.UNAVAILABLE))
            }

            else -> {
                flow {
                    slots.within(settings.deadline) {
                        consult(AssistPrompts.request(idea, mode, candidates), mode, candidates).collect { emit(it) }
                    }
                }
            }
        }

    /** Streams the model's answer: each finished suggestion is checked and passed on, a flow is checked at the end. */
    private fun consult(
        request: LlmRequest,
        mode: AssistMode,
        candidates: List<InnovationItem>,
    ): Flow<Generated> =
        flow {
            val answer =
                llm.stream(request).fold(StreamedAnswer.START) { answer, piece ->
                    val next = answer.after(piece)
                    if (mode != AssistMode.FLOW) {
                        next.finished.forEach { raw ->
                            AssistParser
                                .suggestion(
                                    raw,
                                    candidates,
                                ).onRight { emit(Generated.Produced(AssistEvent.Suggestion(it))) }
                        }
                    }
                    next
                }
            emit(
                when {
                    answer.failure != null -> Generated.Ended(answer.failure.aiStatus())
                    mode == AssistMode.FLOW -> endOfFlow(answer.text)
                    else -> Generated.Ended(AiStatus.OK)
                },
            )
        }

    private fun endOfFlow(text: String): Generated =
        AssistParser.flow(text).fold(
            ifLeft = { Generated.Ended(AiStatus.INVALID_OUTPUT) },
            ifRight = { Generated.Produced(AssistEvent.Flow(it)) },
        )

    private fun noveltyOf(bestScore: Double?): NoveltyHint =
        when {
            bestScore == null -> NoveltyHint.NEW
            bestScore >= ALREADY_EXISTS_SCORE -> NoveltyHint.ALREADY_EXISTS
            else -> NoveltyHint.PARTIAL
        }

    companion object {
        val DEFAULT_DEADLINE: Duration = 25.seconds

        /**
         * Relevance of `POST /api/matches` from which an innovation counts as the same idea. Measured with the hybrid
         * engine on the seed: ideas paraphrasing an innovation score 0.77-0.80, merely related ones 0.45-0.48.
         */
        const val ALREADY_EXISTS_SCORE = 0.65
    }
}
