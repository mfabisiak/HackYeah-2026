package io.github.mfabisiak.hubmi.matching

/** How much a mention in each field of an innovation counts towards its relevance (a weight of 2 = two mentions). */
data class FieldWeights(
    val title: Double = 3.0,
    val keywords: Double = 3.0,
    val summary: Double = 2.0,
    val description: Double = 1.0,
    val problemDiagnosis: Double = 2.0,
    val audienceDescription: Double = 1.0,
    val expectedChange: Double = 0.5,
    val innovativeness: Double = 0.25,
    val futureVision: Double = 0.25,
)
