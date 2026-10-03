package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError
import kotlin.math.min

/**
 * Text (BM25) and meaning (embedding cosine) together: an innovation is relevant when it shares words with the
 * description or says something close to it, so "brak dojazdu do lekarza" finds a transport service that never uses
 * those words. Needs a running [embedder]; [FallbackEngine] covers for it.
 */
class HybridMatchingEngine(
    private val index: InnovationIndex,
    private val vectors: VectorIndex,
    private val embedder: EmbeddingClient,
    private val keyword: KeywordMatchingEngine,
    private val config: Config = Config(),
) : MatchingEngine {
    /**
     * Relevance is `(1 - lexicalWeight) * semantic + lexicalWeight * lexical`, both in `0..1`. The semantic part maps
     * the cosine from [cosineFloor] (unrelated text, 0) to [cosineCeiling] (near paraphrase, 1); both are calibrated
     * on the golden set (docs/matching-baseline.md) and tied to the embedding model.
     */
    data class Config(
        val minScore: Double = DEFAULT_MIN_SCORE,
        val maxResults: Int = KeywordMatchingEngine.DEFAULT_MAX_RESULTS,
        val relativeCutoff: Double = KeywordMatchingEngine.DEFAULT_RELATIVE_CUTOFF,
        val regionBoost: Double = KeywordMatchingEngine.DEFAULT_REGION_BOOST,
        val lexicalWeight: Double = DEFAULT_LEXICAL_WEIGHT,
        val cosineFloor: Double = DEFAULT_COSINE_FLOOR,
        val cosineCeiling: Double = DEFAULT_COSINE_CEILING,
    )

    override suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult> =
        either {
            val snapshot = index.snapshot().bind()
            val queryVector = embedQuery(query).bind()
            val documentVectors = vectors.snapshotFor(snapshot).mapLeft(EmbeddingError::toDomainError).bind()
            val lexical = keyword.lexicalHits(snapshot, query).associateBy { it.innovation.id }
            EngineResult(
                documentVectors
                    .rank(queryVector)
                    .mapNotNull { hit ->
                        snapshot.innovations[hit.key]?.let { innovation ->
                            val lexicalHit = lexical[hit.key]
                            val semantic = semanticScore(hit.cosine)
                            val terms = lexicalHit?.matchedTerms.orEmpty()
                            val regionMatches = innovation.matchesRegion(municipality)
                            val blended =
                                (1 - config.lexicalWeight) * semantic +
                                    config.lexicalWeight * (lexicalHit?.score ?: 0.0)
                            ScoredInnovation(
                                innovation = innovation,
                                score = min(1.0, blended * if (regionMatches) config.regionBoost else 1.0),
                                matchedTerms = terms,
                                reasons =
                                    MatchReasons.of(
                                        innovation = innovation,
                                        matchedTerms = terms,
                                        municipality = municipality,
                                        regionMatches = regionMatches,
                                        semanticallyClose = semantic >= SEMANTIC_REASON_SCORE,
                                    ),
                            )
                        }
                    }.cutOff(config.minScore, config.relativeCutoff, config.maxResults),
            )
        }

    override suspend fun warmUp(): Either<DomainError, Unit> =
        either {
            val snapshot = index.snapshot().bind()
            vectors.snapshotFor(snapshot).mapLeft(EmbeddingError::toDomainError).bind()
        }.map { }

    private suspend fun embedQuery(query: String): Either<DomainError, Embedding> =
        either {
            val embeddings = embedder.embed(listOf(query)).mapLeft(EmbeddingError::toDomainError).bind()
            ensureNotNull(embeddings.singleOrNull()) { EmbeddingError.InvalidResponse("brak wektora").toDomainError() }
        }

    private fun semanticScore(cosine: Double): Double =
        ((cosine - config.cosineFloor) / (config.cosineCeiling - config.cosineFloor)).coerceIn(0.0, 1.0)

    companion object {
        const val DEFAULT_MIN_SCORE = 0.25
        const val DEFAULT_LEXICAL_WEIGHT = 0.4
        const val DEFAULT_COSINE_FLOOR = 0.35
        const val DEFAULT_COSINE_CEILING = 0.75
        const val SEMANTIC_REASON_SCORE = 0.4
    }
}
