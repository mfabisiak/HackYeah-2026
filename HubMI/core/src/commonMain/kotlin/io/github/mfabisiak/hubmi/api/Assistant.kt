package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Module 3 (idea creator): the AI assistant, see docs/ASSISTANT.md. Everything here is a proposal made by a local
// model, so every response says so (`aiGenerated`) and the deterministic part (`similar`) never depends on the model.

/** What the assistant does with an idea. [SIMILAR] needs no model at all. */
@Serializable
enum class AssistMode {
    /** Only the existing innovations closest to the idea and whether it is already covered. */
    SIMILAR,

    /** Ways to develop the idea further. */
    EXPAND,

    /** Risks and assumptions to check before starting. */
    RISKS,

    /** How the idea works as a flow between its actors. */
    FLOW,
}

/** How far the model's part got; the deterministic [AssistResponse.similar] is there in every case. */
@Serializable
enum class AiStatus {
    OK,

    /** The model is off, unreachable, busy or too slow. */
    UNAVAILABLE,

    /** The model answered, but nothing of it passed validation. */
    INVALID_OUTPUT,

    /** [AssistMode.SIMILAR] asks for no model. */
    NOT_REQUESTED,
}

/** Whether the idea is already covered by the library, from how close its best match is. */
@Serializable
enum class NoveltyHint {
    /** A very close innovation exists: consider adapting it instead of starting from scratch. */
    ALREADY_EXISTS,

    /** Something related exists, the idea adds to it. */
    PARTIAL,

    /** Nothing close in the library: a gap an admin may want to know about. */
    NEW,
}

/** Body of `POST /api/ideas/{id}/assist`: the idea is the saved one. */
@Serializable
data class AssistRequest(
    val mode: AssistMode,
)

/** Body of `POST /api/ideas/assist`: the idea as typed in the form, validated like [CreateIdeaRequest]. */
@Serializable
data class AssistDraftRequest(
    val mode: AssistMode,
    val idea: CreateIdeaRequest,
)

@Serializable
data class SimilarInnovationDto(
    val innovation: InnovationSummary,
    /** Closeness in `0..1`, the relevance of `POST /api/matches`. */
    val score: Double,
)

@Serializable
data class SuggestionDto(
    val title: String,
    val rationale: String,
    val nextStep: String,
    /** Innovations of [AssistResponse.similar] the suggestion draws on; ids, never taken on trust from the model. */
    val basedOnInnovationIds: List<String>,
)

@Serializable
data class FlowStepDto(
    /** An entry of [FlowDto.actors]. */
    val from: String,
    /** An entry of [FlowDto.actors]. */
    val to: String,
    val label: String,
)

/** The idea as a sequence of steps between actors; the client draws it and reads it out as a numbered list. */
@Serializable
data class FlowDto(
    val actors: List<String>,
    val steps: List<FlowStepDto>,
)

@Serializable
data class AssistResponse(
    val mode: AssistMode,
    val aiStatus: AiStatus,
    /** Always true: marks everything under [suggestions] and [flow] as generated, to be shown as such. */
    val aiGenerated: Boolean = true,
    val similar: List<SimilarInnovationDto>,
    val noveltyHint: NoveltyHint,
    val suggestions: List<SuggestionDto>,
    val flow: FlowDto?,
)

/**
 * One step of an assist answer streamed as server-sent events (`event:` is the [SerialName] of the case, which the
 * constants of the companion spell out, and `data:` the JSON of the event). A stream always ends with [Done], which carries the whole [AssistResponse]; failures that happen
 * before the stream starts (validation, access) are ordinary error responses instead.
 */
@Serializable
sealed interface AssistEvent {
    /** Sent first and at once: the deterministic part. */
    @Serializable
    @SerialName(AssistEvent.SIMILAR)
    data class Similar(
        val similar: List<SimilarInnovationDto>,
        val noveltyHint: NoveltyHint,
    ) : AssistEvent

    @Serializable
    @SerialName(AssistEvent.SUGGESTION)
    data class Suggestion(
        val suggestion: SuggestionDto,
    ) : AssistEvent

    @Serializable
    @SerialName(AssistEvent.FLOW)
    data class Flow(
        val flow: FlowDto,
    ) : AssistEvent

    @Serializable
    @SerialName(AssistEvent.DONE)
    data class Done(
        val response: AssistResponse,
    ) : AssistEvent

    companion object {
        const val SIMILAR = "similar"
        const val SUGGESTION = "suggestion"
        const val FLOW = "flow"
        const val DONE = "done"
    }
}
