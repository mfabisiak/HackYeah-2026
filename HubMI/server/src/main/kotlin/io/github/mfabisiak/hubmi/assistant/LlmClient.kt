package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.flow.Flow

interface LlmClient {
    /**
     * The answer to [request] as it is generated, piece by piece. A failure is the last element (a [Either.Left]);
     * nothing follows it. Cancelling the collection stops the generation.
     */
    fun stream(request: LlmRequest): Flow<Either<LlmError, String>>

    /** Loads the model, so that the first [stream] does not wait for it. */
    suspend fun warmUp(): Either<LlmError, Unit> = Unit.right()
}
