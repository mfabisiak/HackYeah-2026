package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.innovations.InnovationItem

/** Polish, template-based explanations built only from fields of the innovation (at most three per match). */
object MatchReasons {
    private const val MAX_REASONS = 3
    private const val MAX_LISTED = 4
    private const val MAX_LABELS = 2

    fun of(
        innovation: InnovationItem,
        matchedTerms: List<String>,
        municipality: MunicipalityName?,
        regionMatches: Boolean,
    ): List<String> {
        val terms = matchedTerms.take(MAX_LISTED).joinToString(", ")
        val areas = innovation.areas.take(MAX_LABELS).joinToString(", ") { it.polishLabel() }
        val groups = innovation.targetGroups.take(MAX_LABELS).joinToString(", ") { it.polishLabel() }
        return listOfNotNull(
            if (matchedTerms.isEmpty()) null else "Dopasowane frazy: $terms.",
            "Obszar: $areas.",
            if (regionMatches && municipality != null) {
                "Region: ${innovation.region} (zgodny z podaną gminą ${municipality.value})."
            } else {
                "Grupa docelowa: $groups."
            },
        ).take(MAX_REASONS)
    }
}
