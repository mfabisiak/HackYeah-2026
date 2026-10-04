package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FlowDto
import io.github.mfabisiak.hubmi.api.FlowStepDto
import io.github.mfabisiak.hubmi.api.SuggestionDto
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import kotlinx.serialization.Serializable

/**
 * The model's answer is data to check, not to trust: a schema keeps the JSON well-formed, and this keeps it sensible.
 * It rejects what is blank or too long, takes numbers of innovations only if they exist, and drops the steps of a flow
 * that name an actor nobody listed.
 */
object AssistParser {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_TEXT_LENGTH = 400
    const val MAX_ACTOR_LENGTH = 40
    const val MAX_LABEL_LENGTH = 80
    const val MIN_ACTORS = 2
    const val MAX_ACTORS = 6
    const val MIN_STEPS = 2
    const val MAX_STEPS = 8

    @Serializable
    private data class RawSuggestion(
        val title: String,
        val rationale: String,
        val nextStep: String,
        val basedOn: List<Int> = emptyList(),
    )

    @Serializable
    private data class RawStep(
        val from: String,
        val to: String,
        val label: String,
    )

    @Serializable
    private data class RawFlow(
        val actors: List<String>,
        val steps: List<RawStep>,
    )

    /** [candidates] are the innovations in the order they were numbered in the prompt. */
    fun suggestion(
        raw: String,
        candidates: List<InnovationItem>,
    ): Either<ModelOutputError, SuggestionDto> =
        either {
            val parsed = ModelOutput.decode<RawSuggestion>(raw).bind()
            SuggestionDto(
                title = ModelOutput.text("title", parsed.title, MAX_TITLE_LENGTH).bind(),
                rationale = ModelOutput.text("rationale", parsed.rationale, MAX_TEXT_LENGTH).bind(),
                nextStep = ModelOutput.text("nextStep", parsed.nextStep, MAX_TEXT_LENGTH).bind(),
                basedOnInnovationIds =
                    parsed.basedOn
                        .distinct()
                        .mapNotNull { number -> candidates.getOrNull(number - 1) }
                        .map { it.id.toHexString() },
            )
        }

    fun flow(raw: String): Either<ModelOutputError, FlowDto> =
        either {
            val parsed = ModelOutput.decode<RawFlow>(raw).bind()
            val actors =
                parsed.actors
                    .map(String::trim)
                    .filter { it.isNotEmpty() && it.length <= MAX_ACTOR_LENGTH }
                    .distinctBy(String::lowercase)
            val steps =
                parsed.steps.mapNotNull { step ->
                    val from = actors.firstOrNull { it.equals(step.from.trim(), ignoreCase = true) }
                    val to = actors.firstOrNull { it.equals(step.to.trim(), ignoreCase = true) }
                    val label = step.label.trim()
                    if (from == null || to == null || label.isEmpty() || label.length > MAX_LABEL_LENGTH) {
                        null
                    } else {
                        FlowStepDto(from = from, to = to, label = label)
                    }
                }
            ensure(
                actors.size in MIN_ACTORS..MAX_ACTORS,
            ) { ModelOutputError.Unusable("Liczba aktorów: ${actors.size}") }
            ensure(
                steps.size in MIN_STEPS..MAX_STEPS,
            ) { ModelOutputError.Unusable("Liczba poprawnych kroków: ${steps.size}") }
            FlowDto(actors = actors, steps = steps)
        }
}
