package io.github.mfabisiak.hubmi.assistant

import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.AssistEvent

/** A step of the model's part of an answer. */
internal sealed interface Generated {
    /** A suggestion or a flow that passed validation, to be sent on at once. */
    data class Produced(
        val event: AssistEvent,
    ) : Generated

    /** The model is done, however it went; [status] is how, and matters only if nothing was [Produced]. */
    data class Ended(
        val status: AiStatus,
    ) : Generated
}
