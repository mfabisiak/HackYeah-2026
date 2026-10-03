package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.Catalogue
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.garden
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.loneliness
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.transport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class VectorIndexTest {
    private val catalogue = Catalogue(listOf(transport, garden))
    private val engines = HybridTestFixtures.engines(catalogue)

    private fun source() =
        assertIs<Either.Right<InnovationIndex.Snapshot>>(runBlocking { engines.index.snapshot() }).value

    private fun vectors(source: InnovationIndex.Snapshot) =
        assertIs<Either.Right<VectorIndex.Snapshot>>(runBlocking { engines.vectors.snapshotFor(source) }).value

    @Test
    fun theSameSnapshotIsEmbeddedOnlyOnce() {
        val source = source()

        assertSame(vectors(source), vectors(source))
        assertEquals(1, engines.embedder.calls.get())
    }

    @Test
    fun aRebuiltCatalogueReEmbedsOnlyTheInnovationsThatChanged() {
        vectors(source())
        catalogue.replace(listOf(transport, garden, loneliness))
        engines.index.invalidate()

        vectors(source())

        assertEquals(2 + 1, engines.embedder.embeddedTexts.get(), "two at the start, then only the new one")
        assertEquals(2, engines.embedder.calls.get())
    }

    @Test
    fun anEditedInnovationIsEmbeddedAgain() {
        vectors(source())
        catalogue.replace(listOf(transport.copy(summary = "Zupełnie inny opis"), garden))
        engines.index.invalidate()

        vectors(source())

        assertEquals(2 + 1, engines.embedder.embeddedTexts.get())
    }

    @Test
    fun anUnchangedCatalogueRebuiltByTheTtlCostsNoEmbeddingCall() {
        vectors(source())
        engines.index.invalidate()

        vectors(source())

        assertEquals(1, engines.embedder.calls.get())
    }

    @Test
    fun aFailedEmbeddingIsAnErrorAndLeavesNothingCachedSoTheNextCallRetries() {
        engines.embedder.down.set(true)
        assertIs<EmbeddingError.Unavailable>(runBlocking { engines.vectors.snapshotFor(source()) }.leftOrNull())

        engines.embedder.down.set(false)

        assertIs<Either.Right<VectorIndex.Snapshot>>(runBlocking { engines.vectors.snapshotFor(source()) })
    }

    @Test
    fun ranksByCosineWithTheClosestFirst() {
        val query =
            engines.embedder
                .let {
                    runBlocking {
                        it.embed(
                            listOf("dojazd do przychodni"),
                        )
                    }
                }.getOrNull()!!
                .single()

        val ranked = vectors(source()).rank(query)

        assertEquals(transport.id, ranked.first().key)
        assertEquals(ranked.map { it.cosine }.sortedDescending(), ranked.map { it.cosine })
    }
}
