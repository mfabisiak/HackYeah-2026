package io.github.mfabisiak.hubmi.assistant

import io.github.mfabisiak.hubmi.api.AiStatus

sealed interface LlmError {
    /** The model did not answer (not running, model not pulled, timeout, non-success status). */
    data class Unavailable(
        val cause: Throwable? = null,
        val detail: String? = null,
    ) : LlmError

    /** The service answered, but not in the shape of its protocol. */
    data class InvalidResponse(
        val detail: String,
    ) : LlmError
}

/** How a failing model shows in the answer of the API. */
fun LlmError.aiStatus(): AiStatus =
    when (this) {
        is LlmError.Unavailable -> AiStatus.UNAVAILABLE
        is LlmError.InvalidResponse -> AiStatus.INVALID_OUTPUT
    }
