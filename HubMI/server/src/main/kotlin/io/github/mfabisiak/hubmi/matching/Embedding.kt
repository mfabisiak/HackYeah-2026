package io.github.mfabisiak.hubmi.matching

import kotlin.math.sqrt

/** A text embedding; compared by [cosine], so the vectors need not be normalised. */
@JvmInline
value class Embedding(
    val values: List<Float>,
) {
    /** Cosine similarity in `-1..1`; `0` when the dimensions differ or one of the vectors is zero. */
    fun cosine(other: Embedding): Double {
        val dot = values.zip(other.values) { a, b -> a.toDouble() * b }.sum()
        val norms = sqrt(squaredNorm()) * sqrt(other.squaredNorm())
        return if (values.size != other.values.size || norms == 0.0) 0.0 else dot / norms
    }

    private fun squaredNorm(): Double = values.sumOf { it.toDouble() * it }
}
