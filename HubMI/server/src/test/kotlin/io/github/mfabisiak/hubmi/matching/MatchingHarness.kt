package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.right
import io.github.mfabisiak.hubmi.common.mongo.matches
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.seeding.SeedInnovationItem
import io.github.mfabisiak.hubmi.seeding.seedObjectId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.assertIs

@Serializable
data class GoldenCase(
    val query: String,
    val expected: List<String>,
    val negative: Boolean,
    val register: String,
)

data class Hit(
    val slug: String,
    val score: Double,
    val matchedTerms: List<String>,
)

/** Seeded innovations returned for one golden case, best first. */
data class CaseResult(
    val case: GoldenCase,
    val hits: List<Hit>,
)

data class Metrics(
    val hitAt3: Double,
    val mrr: Double,
    val negativesRejected: Double,
)

/** Measures the engine on the golden set against the seeded innovations (no database needed). */
object MatchingHarness {
    private val json = Json { ignoreUnknownKeys = true }
    private const val SEED_TIMESTAMP = "2026-01-01T00:00:00Z"

    private val seedItems = json.decodeFromString<List<SeedInnovationItem>>(resource("seed/innovations.json"))

    val seed: List<InnovationItem> =
        seedItems.map { item ->
            InnovationItem(
                id = seedObjectId(item.slug),
                title = item.title,
                summary = item.summary,
                description = item.description,
                areas = item.areas,
                targetGroups = item.targetGroups,
                stage = item.stage,
                region = item.region,
                keywords = item.keywords,
                mediaUrls = item.mediaUrls,
                innovativeness = item.innovativeness,
                problemDiagnosis = item.problemDiagnosis,
                audienceDescription = item.audienceDescription,
                expectedChange = item.expectedChange,
                futureVision = item.futureVision,
                createdAt = SEED_TIMESTAMP,
                updatedAt = SEED_TIMESTAMP,
            )
        }

    val golden: List<GoldenCase> = json.decodeFromString(resource("matching/golden.json"))

    private val slugById = seedItems.associate { seedObjectId(it.slug) to it.slug }

    val knownSlugs: Set<String> = slugById.values.toSet()

    /** An engine over the seed that shows every innovation with a positive score (the threshold is applied later). */
    fun engine(
        stemmer: Stemmer,
        weights: FieldWeights = FieldWeights(),
    ): KeywordMatchingEngine {
        val analyzer = TextAnalyzer(stemmer)
        return KeywordMatchingEngine(
            index = InnovationIndex(analyzer, load = { seed.right() }, weights = weights),
            analyzer = analyzer,
            config = KeywordMatchingEngine.Config(minScore = 0.0, maxResults = seed.size),
        )
    }

    fun run(engine: KeywordMatchingEngine): List<CaseResult> =
        golden.map { case ->
            val result = assertIs<Either.Right<EngineResult>>(runBlocking { engine.match(case.query, null) }).value
            CaseResult(case, result.matches.map { Hit(slugById.getValue(it.innovation.id), it.score, it.matchedTerms) })
        }

    /** Applies the production cut-off (relevance threshold, at most five matches) to unfiltered results. */
    fun metrics(
        results: List<CaseResult>,
        minScore: Double,
        maxResults: Int = KeywordMatchingEngine.DEFAULT_MAX_RESULTS,
    ): Metrics {
        val shown = results.map { it to it.hits.filter { hit -> hit.score >= minScore }.take(maxResults) }
        val positives = shown.filter { !it.first.case.negative }
        val negatives = shown.filter { it.first.case.negative }
        val ranks =
            positives.map { (result, hits) ->
                hits.indexOfFirst { it.slug in result.case.expected }.takeIf { it >= 0 }?.plus(1)
            }
        return Metrics(
            hitAt3 = ranks.count { it != null && it <= 3 }.toDouble() / positives.size,
            mrr = ranks.sumOf { rank -> rank?.let { 1.0 / it } ?: 0.0 } / positives.size,
            negativesRejected = negatives.count { (_, hits) -> hits.isEmpty() }.toDouble() / negatives.size,
        )
    }

    private fun resource(path: String): String =
        MatchingHarness::class.java.classLoader
            .getResourceAsStream(path)
            .let { assertIs<java.io.InputStream>(it) }
            .bufferedReader()
            .use { it.readText() }
}
