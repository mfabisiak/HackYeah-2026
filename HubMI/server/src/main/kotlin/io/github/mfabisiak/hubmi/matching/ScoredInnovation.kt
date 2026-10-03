package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.api.MatchDto
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
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

fun InnovationItem.matchesRegion(municipality: MunicipalityName?): Boolean =
    municipality != null && region?.contains(municipality.value, ignoreCase = true) == true

/**
 * What is shown to the user: matches at or above [minScore] and at least [relativeCutoff] of the best one, so a weak
 * tail next to a clear winner is dropped; at most [maxResults], best first.
 */
fun List<ScoredInnovation>.cutOff(
    minScore: Double,
    relativeCutoff: Double,
    maxResults: Int,
): List<ScoredInnovation> {
    val sorted = sortedByDescending(ScoredInnovation::score)
    val floor = maxOf(minScore, (sorted.firstOrNull()?.score ?: 0.0) * relativeCutoff)
    return sorted.filter { it.score >= floor }.take(maxResults)
}
