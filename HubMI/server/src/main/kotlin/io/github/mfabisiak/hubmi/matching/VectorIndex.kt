package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.types.ObjectId
import java.util.concurrent.atomic.AtomicReference

/**
 * Embeddings of the innovations of one [InnovationIndex.Snapshot], kept in memory. A new snapshot reuses the vectors of
 * innovations whose text did not change and embeds only the rest in one call, so rebuilding the index every few
 * minutes costs nothing unless the catalogue was edited.
 */
class VectorIndex(
    private val embedder: EmbeddingClient,
) {
    class Snapshot(
        private val vectors: Map<ObjectId, EmbeddedText>,
    ) {
        fun rank(query: Embedding): List<VectorHit> =
            vectors
                .map { (id, embedded) -> VectorHit(id, embedded.embedding.cosine(query)) }
                .sortedByDescending(VectorHit::cosine)

        internal fun embeddedTexts(): Map<ObjectId, EmbeddedText> = vectors
    }

    class EmbeddedText(
        val text: String,
        val embedding: Embedding,
    )

    private class Built(
        val source: InnovationIndex.Snapshot,
        val vectors: Snapshot,
    )

    private val current = AtomicReference<Built?>(null)
    private val rebuild = Mutex()

    suspend fun snapshotFor(source: InnovationIndex.Snapshot): Either<EmbeddingError, Snapshot> =
        fresh(source)?.right()
            ?: rebuild.withLock {
                fresh(source)?.right()
                    ?: build(source).map { Built(source, it) }.onRight(current::set).map(Built::vectors)
            }

    private fun fresh(source: InnovationIndex.Snapshot): Snapshot? =
        current.get()?.takeIf { it.source === source }?.vectors

    private suspend fun build(source: InnovationIndex.Snapshot): Either<EmbeddingError, Snapshot> {
        val previous =
            current
                .get()
                ?.vectors
                ?.embeddedTexts()
                .orEmpty()
        val texts = source.innovations.mapValues { (_, innovation) -> innovation.embeddingText() }
        val (reusable, missing) = texts.entries.partition { (id, text) -> previous[id]?.text == text }
        val reused = reusable.associate { (id, _) -> id to previous.getValue(id) }
        return if (missing.isEmpty()) {
            Snapshot(reused).right()
        } else {
            embedder.embed(missing.map { it.value }).map { embeddings ->
                Snapshot(
                    reused + missing.zip(embeddings) { (id, text), embedding -> id to EmbeddedText(text, embedding) },
                )
            }
        }
    }
}
