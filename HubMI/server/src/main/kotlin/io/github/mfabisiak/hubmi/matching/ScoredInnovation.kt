package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.api.MatchDto
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.toSummary

data class ScoredInnovation(
    val innovation: InnovationItem,
    /** Relevance in `0..1`. */
    val score: Double,
    /** Words of the query (as typed) whose stems were found in the innovation. */
    val matchedTerms: List<String>,
    val reasons: List<String>,
)

fun ScoredInnovation.toDto(): MatchDto =
    MatchDto(innovation = innovation.toSummary(), score = score, reasons = reasons, matchedTerms = matchedTerms)
