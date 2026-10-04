package io.github.mfabisiak.hubmi.assistant

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * The places at the local model, shared by everything that asks it for something. A local model generates for one
 * request at a time (or a few), so the others wait their turn; no caller waits longer than its own deadline.
 */
class LlmSlots(
    maxParallel: Int = 1,
) {
    private val slots = Semaphore(maxParallel)

    /** Runs [block] once a place is free; `null` when [deadline] ran out first, counting from now, in the queue or in [block]. */
    suspend fun <T : Any> within(
        deadline: Duration,
        block: suspend () -> T,
    ): T? = withTimeoutOrNull(deadline) { slots.withPermit { block() } }
}
