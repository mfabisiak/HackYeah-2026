package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.types.ObjectId
import java.util.concurrent.atomic.AtomicReference

/**
 * Embeddings of the innovations of one [InnovationIndex.Snapshot], kept in memory. Vectors are remembered by text, so
 * a rebuilt index embeds only texts it has not seen: nothing after the five-minute refresh, one innovation after an
 * edit.
 *
 * Texts go to the embedder in batches of [batchSize], and every finished batch is kept even when a later one fails.
 * On a slow machine (Ollama on a CPU needs about a second per text) one request for the whole catalogue would exceed
 * the client timeout every time; in small batches each attempt makes progress, so the build converges.
 */
class VectorIndex(
    private val embedder: EmbeddingClient,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    class Snapshot(
        private val vectors: Map<ObjectId, Embedding>,
    ) {
        fun rank(query: Embedding): List<VectorHit> =
            vectors
                .map { (id, embedding) -> VectorHit(id, embedding.cosine(query)) }
                .sortedByDescending(VectorHit::cosine)
    }

    private class Built(
        val source: InnovationIndex.Snapshot,
        val vectors: Snapshot,
    )

    private val current = AtomicReference<Built?>(null)
    private val embedded = AtomicReference<Map<String, Embedding>>(emptyMap())
    private val rebuild = Mutex()

    suspend fun snapshotFor(source: InnovationIndex.Snapshot): Either<EmbeddingError, Snapshot> =
        fresh(source)?.right()
            ?: rebuild.withLock {
                fresh(source)?.right()
                    ?: build(source).map { Built(source, it) }.onRight(current::set).map(Built::vectors)
            }

    private fun fresh(source: InnovationIndex.Snapshot): Snapshot? =
        current.get()?.takeIf { it.source === source }?.vectors

    private suspend fun build(source: InnovationIndex.Snapshot): Either<EmbeddingError, Snapshot> =
        either {
            val texts = source.innovations.mapValues { (_, innovation) -> innovation.embeddingText() }
            val wanted = texts.values.toSet()
            embedMissing(wanted).bind()
            val known = embedded.updateAndGet { it.filterKeys(wanted::contains) }
            Snapshot(texts.mapNotNull { (id, text) -> known[text]?.let { id to it } }.toMap())
        }

    private suspend fun embedMissing(wanted: Set<String>): Either<EmbeddingError, Unit> =
        either {
            wanted
                .filter { it !in embedded.get() }
                .chunked(batchSize)
                .forEach { batch ->
                    val vectors = embedder.embed(batch).bind()
                    embedded.updateAndGet { it + batch.zip(vectors) }
                }
        }

    companion object {
        const val DEFAULT_BATCH_SIZE = 4
    }
}
