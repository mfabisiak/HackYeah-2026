package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.mongo.matches
import io.github.mfabisiak.hubmi.innovations.innovations
import kotlin.math.min

/** BM25 over the normalised innovation fields; needs no models, so it is also the fallback of the other engines. */
class KeywordMatchingEngine(
    private val index: InnovationIndex,
    private val analyzer: TextAnalyzer,
    private val config: Config = Config(),
) : MatchingEngine {
    /**
     * @property minScore relevance (`0..1`) below which an innovation is not shown; calibrated on the golden set
     * (see docs/matching-baseline.md).
     * @property relativeCutoff share of the best score an innovation needs to be shown, so a weak tail next to a
     * clear winner is dropped (the threshold alone lets queries full of generic words through).
     * @property regionBoost factor applied to innovations whose region contains the municipality given by the user.
     */
    data class Config(
        val minScore: Double = DEFAULT_MIN_SCORE,
        val maxResults: Int = DEFAULT_MAX_RESULTS,
        val relativeCutoff: Double = DEFAULT_RELATIVE_CUTOFF,
        val regionBoost: Double = DEFAULT_REGION_BOOST,
    )

    override suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult> = index.snapshot().map { rank(it, query, municipality) }

    private fun rank(
        snapshot: InnovationIndex.Snapshot,
        query: String,
        municipality: MunicipalityName?,
    ): EngineResult {
        val tokens = analyzer.analyze(query)
        val matches =
            snapshot.index
                .search(tokens.map(Token::stem))
                .mapNotNull { hit ->
                    snapshot.innovations[hit.key]?.let { innovation ->
                        val regionMatches =
                            municipality != null &&
                                innovation.region?.contains(municipality.value, ignoreCase = true) == true
                        val matchedTerms =
                            tokens.filter { it.stem in hit.matchedStems }.map(Token::surface).distinct()
                        ScoredInnovation(
                            innovation = innovation,
                            score = min(1.0, hit.score * if (regionMatches) config.regionBoost else 1.0),
                            matchedTerms = matchedTerms,
                            reasons = MatchReasons.of(innovation, matchedTerms, municipality, regionMatches),
                        )
                    }
                }.filter { it.score >= config.minScore }
                .sortedByDescending { it.score }
        val cutoff = (matches.firstOrNull()?.score ?: 0.0) * config.relativeCutoff
        return EngineResult(matches.filter { it.score >= cutoff }.take(config.maxResults))
    }

    companion object {
        const val DEFAULT_MIN_SCORE = 0.18
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_RELATIVE_CUTOFF = 0.7
        const val DEFAULT_REGION_BOOST = 1.15
    }
}
