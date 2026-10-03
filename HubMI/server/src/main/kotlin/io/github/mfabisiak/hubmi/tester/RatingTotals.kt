package io.github.mfabisiak.hubmi.tester

/** Sum and number of the ratings of one innovation, recomputed from the individual feedback documents. */
data class RatingTotals(
    val sum: Int,
    val count: Int,
)
