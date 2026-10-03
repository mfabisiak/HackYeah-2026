package io.github.mfabisiak.hubmi.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmbeddingTest {
    private fun embedding(vararg values: Float) = Embedding(values.toList())

    @Test
    fun identicalDirectionsHaveCosineOneRegardlessOfLength() {
        assertEquals(1.0, embedding(1f, 2f, 3f).cosine(embedding(2f, 4f, 6f)), 1e-9)
    }

    @Test
    fun orthogonalVectorsHaveCosineZero() {
        assertEquals(0.0, embedding(1f, 0f).cosine(embedding(0f, 1f)), 1e-9)
    }

    @Test
    fun oppositeVectorsHaveNegativeCosine() {
        assertTrue(embedding(1f, 0f).cosine(embedding(-1f, 0f)) < 0.0)
    }

    @Test
    fun degenerateInputsGiveZeroInsteadOfNaN() {
        assertEquals(0.0, embedding(0f, 0f).cosine(embedding(1f, 1f)))
        assertEquals(0.0, embedding(1f, 1f).cosine(embedding(1f, 1f, 1f)))
    }
}
