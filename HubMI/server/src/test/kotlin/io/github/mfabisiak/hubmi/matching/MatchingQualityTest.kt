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
    private val hybridResults = MatchingHarness.run(MatchingHarness.hybridEngine())

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
    fun hybridMeetsTheTargetsAndBeatsTheKeywordBaseline() {
        val keyword = MatchingHarness.metrics(productionResults, production)
        val hybrid = MatchingHarness.metrics(hybridResults, HybridMatchingEngine.DEFAULT_MIN_SCORE)

        assertTrue(hybrid.hitAt3 > keyword.hitAt3, "hybrid hit@3 ${hybrid.hitAt3} must beat keyword ${keyword.hitAt3}")
        assertTrue(hybrid.mrr > keyword.mrr, "hybrid MRR ${hybrid.mrr} must beat keyword ${keyword.mrr}")
        assertTrue(hybrid.negativesRejected >= TARGET_NEGATIVES, "Rejected negatives ${hybrid.negativesRejected}")
    }

    @Test
    fun hybridFindsWhatKeywordsMissOnSeniorQueries() {
        val keyword = MatchingHarness.metrics(registerOf(productionResults, "senior"), production)
        val hybrid =
            MatchingHarness.metrics(
                registerOf(hybridResults, "senior"),
                HybridMatchingEngine.DEFAULT_MIN_SCORE,
            )

        assertTrue(hybrid.hitAt3 > keyword.hitAt3, "senior hit@3: hybrid ${hybrid.hitAt3} vs keyword ${keyword.hitAt3}")
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

    private fun registerOf(
        results: List<CaseResult>,
        register: String,
    ) = results.filter { it.case.register == register && !it.case.negative }

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
            >To punkt odniesienia dla silnika hybrydowego (BE-04, sekcja na końcu): hybryda daje wyższe hit@3 przy tym samym zestawie.
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
            >
            >${hybridSection()}
            >""".trimMargin(">")
    }

    private fun hybridSection(): String {
        val keyword = MatchingHarness.metrics(productionResults, production)
        val threshold = HybridMatchingEngine.DEFAULT_MIN_SCORE
        val hybrid = MatchingHarness.metrics(hybridResults, threshold)
        val config = HybridMatchingEngine.Config()
        val registers =
            MatchingHarness.golden
                .filter { !it.negative }
                .map { it.register }
                .distinct()
                .joinToString("\n") { register ->
                    val k = MatchingHarness.metrics(registerOf(productionResults, register), production)
                    val h = MatchingHarness.metrics(registerOf(hybridResults, register), threshold)
                    "| $register | ${registerOf(
                        hybridResults,
                        register,
                    ).size} | ${pct(k.hitAt3)} | ${pct(h.hitAt3)} | " +
                        "${num(k.mrr)} | ${num(h.mrr)} |"
                }
        val misses =
            hybridResults
                .filter { !it.case.negative }
                .filter { result ->
                    result.hits
                        .filter { it.score >= threshold }
                        .take(HIT_RANK)
                        .none { it.slug in result.case.expected }
                }.joinToString("\n") { "- „${it.case.query}” → oczekiwano: ${it.case.expected.joinToString()}" }
                .ifEmpty { "brak" }
        val sweep =
            HYBRID_THRESHOLDS.joinToString(
                "\n",
            ) { "| ${num(it)} | ${row(MatchingHarness.metrics(hybridResults, it))} |" }
        val highestNegative =
            hybridResults.filter { it.case.negative }.maxOfOrNull { it.hits.firstOrNull()?.score ?: 0.0 } ?: 0.0
        val lowestPositive =
            hybridResults
                .filter { !it.case.negative }
                .minOfOrNull { result -> result.hits.firstOrNull { it.slug in result.case.expected }?.score ?: 0.0 }
                ?: 0.0
        return """
            >## Silnik hybrydowy (BE-04)
            >
            >BM25 (j.w.) + embeddingi `bge-m3` z lokalnej Ollamy. Wektory zestawu nagrano do
            >`server/src/test/resources/matching/embeddings.json.gz` (`WRITE_MATCHING_EMBEDDINGS=true`), więc pomiar
            >i test w CI nie wymagają modelu. Trafność: `(1 − w)·semantyka + w·tekst`, `w` = ${num(
            config.lexicalWeight,
        )};
            >semantyka to cosinus przeskalowany z ${num(config.cosineFloor)} (obcy tekst, 0) do
            >${num(config.cosineCeiling)} (niemal parafraza, 1); próg `noGoodMatch` = ${num(threshold)};
            >odcięcie względne ${pct(config.relativeCutoff)} najlepszego wyniku; maks. 5 wyników.
            >
            >| silnik | próg | hit@3 | MRR | negatywy odrzucone |
            >|---|---|---|---|---|
            >| keyword | ${num(production)} | ${row(keyword)} |
            >| hybrid | ${num(threshold)} | ${row(hybrid)} |
            >
            >### Wg rejestru języka
            >
            >| rejestr | zapytań | hit@3 keyword | hit@3 hybrid | MRR keyword | MRR hybrid |
            >|---|---|---|---|---|---|
            >$registers
            >
            >### Chybione zapytania hybrydy
            >
            >$misses
            >
            >### Kalibracja progu hybrydy
            >
            >| próg | hit@3 | MRR | negatywy odrzucone |
            >|---|---|---|---|
            >$sweep
            >
            >Najniższy wynik trafnej innowacji w zestawie: ${num(lowestPositive)}; najwyższy wynik negatywu:
            >${num(highestNegative)}. ${hybridThresholdNote(threshold, highestNegative)}
        """.trimMargin(">")
    }

    private fun hybridThresholdNote(
        threshold: Double,
        highestNegative: Double,
    ) = if (threshold > highestNegative) {
        "Próg leży nad najwyższym negatywem."
    } else {
        "Próg leży poniżej najwyższego negatywu: świadomie przepuszczamy tematycznie sąsiednie zapytanie " +
            "(brak w bibliotece, ale blisko „pomocy żywnościowej”), bo wyższy próg kosztuje trafne wyniki."
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
        val HYBRID_THRESHOLDS = (15..45).map { it / 100.0 }
    }
}
