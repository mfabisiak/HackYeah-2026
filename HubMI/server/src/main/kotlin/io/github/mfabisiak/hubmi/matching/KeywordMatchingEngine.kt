package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError
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

    /** Every innovation sharing a stem with [query], most relevant first. */
    fun lexicalHits(
        snapshot: InnovationIndex.Snapshot,
        query: String,
    ): List<LexicalHit> {
        val tokens = analyzer.analyze(query)
        return snapshot.index
            .search(tokens.map(Token::stem))
            .mapNotNull { hit ->
                snapshot.innovations[hit.key]?.let { innovation ->
                    LexicalHit(
                        innovation = innovation,
                        score = hit.score,
                        matchedTerms = tokens.filter { it.stem in hit.matchedStems }.map(Token::surface).distinct(),
                    )
                }
            }
    }

    private fun rank(
        snapshot: InnovationIndex.Snapshot,
        query: String,
        municipality: MunicipalityName?,
    ): EngineResult =
        EngineResult(
            lexicalHits(snapshot, query)
                .map { hit ->
                    val regionMatches = hit.innovation.matchesRegion(municipality)
                    ScoredInnovation(
                        innovation = hit.innovation,
                        score = min(1.0, hit.score * if (regionMatches) config.regionBoost else 1.0),
                        matchedTerms = hit.matchedTerms,
                        reasons = MatchReasons.of(hit.innovation, hit.matchedTerms, municipality, regionMatches),
                    )
                }.cutOff(config.minScore, config.relativeCutoff, config.maxResults),
        )

    companion object {
        const val DEFAULT_MIN_SCORE = 0.18
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_RELATIVE_CUTOFF = 0.7
        const val DEFAULT_REGION_BOOST = 1.15
    }
}
