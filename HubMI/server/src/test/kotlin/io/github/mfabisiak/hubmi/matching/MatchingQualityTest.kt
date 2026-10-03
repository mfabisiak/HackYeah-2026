package io.github.mfabisiak.hubmi.matching

import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Relevance gate of the keyword engine on the golden set (hit@3, MRR, rejected negatives). With
 * `WRITE_MATCHING_BASELINE=true` it also regenerates docs/matching-baseline.md, the baseline for the hybrid engine.
 */
class MatchingQualityTest {
    private val production = KeywordMatchingEngine.DEFAULT_MIN_SCORE
    private val productionResults = MatchingHarness.run(MatchingHarness.engine(PrefixStemmer()))

    @Test
    fun goldenSetReferencesExistingInnovationsAndCoversNegatives() {
        val cases = MatchingHarness.golden
        assertTrue(cases.size in 30..60, "Golden set should have 30-50 cases (got ${cases.size})")
        assertTrue(cases.count { it.negative } >= 5, "Golden set needs negative cases to calibrate the threshold")
        cases.filter { !it.negative }.forEach { case ->
            assertTrue(case.expected.isNotEmpty(), "Positive case without expectation: ${case.query}")
            assertTrue(MatchingHarness.knownSlugs.containsAll(case.expected), "Unknown slug in: ${case.query}")
        }
        cases.filter { it.negative }.forEach { assertTrue(it.expected.isEmpty(), it.query) }
    }

    @Test
    fun productionConfigurationMeetsRelevanceTargets() {
        val metrics = MatchingHarness.metrics(productionResults, production)

        assertTrue(metrics.hitAt3 >= TARGET_HIT_AT_3, "hit@3 ${metrics.hitAt3} below $TARGET_HIT_AT_3")
        assertTrue(
            metrics.negativesRejected >= TARGET_NEGATIVES,
            "Rejected negatives ${metrics.negativesRejected} below $TARGET_NEGATIVES",
        )
    }

    @Test
    fun prefixStemmingBeatsNoStemmingOnTheGoldenSet() {
        val unstemmed = MatchingHarness.run(MatchingHarness.engine(Stemmer { it }))

        assertTrue(
            bestMetrics(productionResults).hitAt3 > bestMetrics(unstemmed).hitAt3,
            "Stemming should improve hit@3 over exact word matching",
        )
    }

    @Test
    fun applicationFormSectionsImproveRelevanceOnTheSeed() {
        val without = MatchingHarness.run(MatchingHarness.engine(PrefixStemmer(), WITHOUT_FORM_SECTIONS))

        assertTrue(
            bestMetrics(productionResults).hitAt3 > bestMetrics(without).hitAt3,
            "Indexing the diagnosis/audience sections should find more of the expected innovations",
        )
    }

    @Test
    fun identicalQueriesGiveIdenticalRanking() {
        val again = MatchingHarness.run(MatchingHarness.engine(PrefixStemmer()))

        assertEquals(productionResults, again, "keyword engine must be deterministic")
    }

    @Test
    fun writeBaselineReport() {
        if (System.getenv("WRITE_MATCHING_BASELINE") != "true") return
        File("../docs/matching-baseline.md").writeText(report())
    }

    /** Best hit@3 over the sweep among thresholds that still reject at least [COMPARISON_NEGATIVES] of the negatives. */
    private fun bestMetrics(results: List<CaseResult>): Metrics =
        THRESHOLDS
            .map { MatchingHarness.metrics(results, it) }
            .filter { it.negativesRejected >= COMPARISON_NEGATIVES }
            .maxByOrNull { it.hitAt3 }
            ?: Metrics(hitAt3 = 0.0, mrr = 0.0, negativesRejected = 0.0)

    private fun report(): String {
        val comparison =
            STEMMERS.map { (name, stemmer) ->
                name to bestOf(MatchingHarness.run(MatchingHarness.engine(stemmer)))
            }
        val comparisonRows = comparison.joinToString("\n") { (name, best) -> "| $name | ${bestRow(best)} |" }
        val winner = comparison.maxByOrNull { (_, best) -> best?.second?.hitAt3 ?: 0.0 }?.first
        val withoutSections = MatchingHarness.run(MatchingHarness.engine(PrefixStemmer(), WITHOUT_FORM_SECTIONS))
        val sectionsRows =
            listOf(
                "z sekcjami formularza (produkcyjnie)" to productionResults,
                "tylko tytuł, słowa kluczowe, streszczenie i opis" to withoutSections,
            ).joinToString("\n") { (name, results) -> "| $name | ${bestRow(bestOf(results))} |" }
        val sweep =
            THRESHOLDS.joinToString("\n") { "| ${num(it)} | ${row(MatchingHarness.metrics(productionResults, it))} |" }
        val registers =
            MatchingHarness.golden
                .filter { !it.negative }
                .map { it.register }
                .distinct()
                .joinToString("\n") { register ->
                    val subset = productionResults.filter { it.case.register == register && !it.case.negative }
                    val m = MatchingHarness.metrics(subset, production)
                    "| $register | ${subset.size} | ${pct(m.hitAt3)} | ${num(m.mrr)} |"
                }
        val misses =
            productionResults
                .filter { !it.case.negative }
                .filter { result ->
                    result.hits
                        .filter { it.score >= production }
                        .take(HIT_RANK)
                        .none { it.slug in result.case.expected }
                }.joinToString("\n") { "- „${it.case.query}” → oczekiwano: ${it.case.expected.joinToString()}" }
        val highestNegative =
            productionResults.filter { it.case.negative }.maxOfOrNull { it.hits.firstOrNull()?.score ?: 0.0 } ?: 0.0
        val thresholdNote =
            if (production > highestNegative) {
                "Próg ${num(production)} leży powyżej najwyższego wyniku negatywu (${num(highestNegative)})."
            } else {
                "Próg ${num(production)} leży poniżej najwyższego wyniku negatywu (${num(highestNegative)}): " +
                    "świadomie przepuszczamy najtrudniejszy negatyw, bo wyższy próg kosztuje hit@3."
            }
        val metrics = MatchingHarness.metrics(productionResults, production)
        val positiveCount = MatchingHarness.golden.count { !it.negative }
        val negativeCount = MatchingHarness.golden.size - positiveCount
        val minNegatives = pct(COMPARISON_NEGATIVES)
        return """
            ># Baza trafności matchmakingu (silnik `keyword`)
            >
            >Plik generuje test `MatchingQualityTest` (`WRITE_MATCHING_BASELINE=true ./gradlew :server:test`).
            >To punkt odniesienia dla silnika hybrydowego (BE-04): hybryda musi dać wyższe hit@3 przy tym samym zestawie.
            >
            >**Zestaw:** $positiveCount zapytań z oczekiwanymi innowacjami
            >+ $negativeCount negatywnych (bez dobrej odpowiedzi), na ${MatchingHarness.seed.size} innowacjach seedowych
            >(`server/src/test/resources/matching/golden.json`).
            >
            >**Konfiguracja produkcyjna:** stemming „pierwsze ${PrefixStemmer.DEFAULT_LENGTH} znaki”, BM25 `k1=1,2`, `b=0,75`,
            >wagi pól: ${weightsDescription(FieldWeights())}; próg `noGoodMatch` = ${num(production)}; maks. 5 wyników.
            >
            >## Wynik
            >
            >| hit@3 | MRR | negatywy odrzucone |
            >|---|---|---|
            >| ${pct(metrics.hitAt3)} | ${num(metrics.mrr)} | ${pct(metrics.negativesRejected)} |
            >
            >Cele z BE-03: hit@3 ≥ ${pct(TARGET_HIT_AT_3)}, ≥ ${pct(TARGET_NEGATIVES)} negatywów odrzuconych.
            >
            >### Wg rejestru języka
            >
            >| rejestr | zapytań | hit@3 | MRR |
            >|---|---|---|---|
            >$registers
            >
            >### Chybione zapytania (brak oczekiwanej innowacji w top 3)
            >
            >$misses
            >
            >## Wpływ sekcji formularza aplikacyjnego ROPS
            >
            >Innowacje niosą sekcje 4–8 formularza (innowacyjność, diagnoza problemu, odbiorcy, zmiana, wizja). Dla każdego
            >wariantu: najlepszy próg spośród tych, które odrzucają ≥ $minNegatives negatywów.
            >
            >| indeksowane pola | próg | hit@3 | MRR | negatywy odrzucone |
            >|---|---|---|---|---|
            >$sectionsRows
            >
            >Uwaga: sekcje seedu napisano na podstawie opisów innowacji, więc zysk jest przy tym zestawie zawyżony;
            >prawdziwe wnioski ROPS trzeba przemierzyć na własnych danych.
            >
            >## Wybór normalizacji polskiego tekstu
            >
            >Dla każdego wariantu: najlepszy próg spośród tych, które odrzucają ≥ $minNegatives negatywów.
            >
            >| wariant | próg | hit@3 | MRR | negatywy odrzucone |
            >|---|---|---|---|---|
            >$comparisonRows
            >
            >Najwyższy hit@3 ma: **$winner**. Obcięcie do kilku znaków nie wymaga zależności i działa także na tekst
            >bez polskich znaków (diakrytyki są składane przy indeksowaniu i w zapytaniu).
            >
            >## Kalibracja progu (wybrany wariant)
            >
            >| próg | hit@3 | MRR | negatywy odrzucone |
            >|---|---|---|---|
            >$sweep
            >
            >Uwaga: zestaw jest mały i pisany przez zespół, więc próg może być przeuczony; przed demo dołożyć zapytania od
            >osób spoza zespołu. $thresholdNote
            >""".trimMargin(">")
    }

    private fun bestOf(results: List<CaseResult>): Pair<Double, Metrics>? =
        THRESHOLDS
            .map { it to MatchingHarness.metrics(results, it) }
            .filter { (_, m) -> m.negativesRejected >= COMPARISON_NEGATIVES }
            .maxByOrNull { (_, m) -> m.hitAt3 }

    private fun bestRow(best: Pair<Double, Metrics>?): String =
        best?.let { (threshold, m) -> "${num(threshold)} | ${row(m)}" } ?: "– | – | – | –"

    private fun weightsDescription(w: FieldWeights): String =
        listOf(
            "tytuł ×${w.title}",
            "słowa kluczowe ×${w.keywords}",
            "streszczenie ×${w.summary}",
            "opis ×${w.description}",
            "diagnoza problemu ×${w.problemDiagnosis}",
            "odbiorcy ×${w.audienceDescription}",
            "zmiana ×${w.expectedChange}",
            "innowacyjność ×${w.innovativeness}",
            "wizja ×${w.futureVision}",
        ).joinToString(", ")

    private fun num(value: Double) = String.format(Locale.forLanguageTag("pl"), "%.2f", value)

    private fun pct(value: Double) = String.format(Locale.forLanguageTag("pl"), "%.0f%%", value * 100)

    private fun row(m: Metrics) = "${num(m.hitAt3)} | ${num(m.mrr)} | ${pct(m.negativesRejected)}"

    private companion object {
        val STEMMERS: List<Pair<String, Stemmer>> =
            listOf(
                "bez stemmingu" to Stemmer { it },
                "prefiks 4 znaków" to PrefixStemmer(4),
                "prefiks 5 znaków" to PrefixStemmer(5),
                "prefiks 6 znaków" to PrefixStemmer(6),
                "prefiks 7 znaków" to PrefixStemmer(7),
                "Lucene Stempel" to StempelPolishStemmer(),
            )
        val WITHOUT_FORM_SECTIONS =
            FieldWeights(
                problemDiagnosis = 0.0,
                audienceDescription = 0.0,
                expectedChange = 0.0,
                innovativeness = 0.0,
                futureVision = 0.0,
            )
        const val TARGET_HIT_AT_3 = 0.7
        const val TARGET_NEGATIVES = 0.8
        const val COMPARISON_NEGATIVES = 0.9
        const val HIT_RANK = 3
        val THRESHOLDS = (5..40).map { it / 100.0 }
    }
}
