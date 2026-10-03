package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Embeds a text as a bag of concepts: one dimension per concept, counting how many of its words occur in the text.
 * That makes "dojazd do lekarza" close to a text about "transport" without sharing a word, which is the whole point
 * of the hybrid engine, and keeps the tests deterministic and model-free.
 */
class FakeEmbeddingClient(
    private val concepts: List<Set<String>>,
) : EmbeddingClient {
    val down = AtomicBoolean(false)

    /** Calls after this many succeed no more, like an embedder that is too slow for the client timeout. */
    val failAfterCalls = AtomicInteger(Int.MAX_VALUE)
    val calls = AtomicInteger(0)
    val embeddedTexts = AtomicInteger(0)

    override suspend fun embed(texts: List<String>): Either<EmbeddingError, List<Embedding>> {
        val call = calls.incrementAndGet()
        return if (down.get() || call > failAfterCalls.get()) {
            EmbeddingError.Unavailable(detail = "fake is down").left()
        } else {
            embeddedTexts.addAndGet(texts.size)
            texts.map(::embedding).right()
        }
    }

    private fun embedding(text: String): Embedding {
        val lower = text.lowercase()
        return Embedding(concepts.map { words -> words.count { it in lower }.toFloat() })
    }
}
