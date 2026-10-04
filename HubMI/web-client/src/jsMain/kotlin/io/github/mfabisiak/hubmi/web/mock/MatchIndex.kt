package io.github.mfabisiak.hubmi.web.mock

import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.SocialArea
import kotlin.math.exp
import kotlin.math.ln

private const val STEM_LENGTH = 5
private const val MIN_TOKEN_LENGTH = 3

/** Polish words too common to say anything about a problem. */
private val STOP_WORDS =
    setOf(
        "oraz",
        "który",
        "która",
        "które",
        "którzy",
        "jest",
        "są",
        "nie",
        "się",
        "dla",
        "przez",
        "jak",
        "ale",
        "czy",
        "tak",
        "tylko",
        "bardzo",
        "mamy",
        "mają",
        "chcemy",
        "chcę",
        "potrzebujemy",
        "gdzie",
        "przy",
        "jego",
        "jej",
        "ich",
        "tego",
        "tych",
        "tym",
        "oraz",
        "także",
        "też",
        "albo",
        "lub",
        "ani",
        "bez",
        "pod",
        "nad",
        "wśród",
    )

internal fun tokensOf(text: String): List<String> =
    text
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.length >= MIN_TOKEN_LENGTH && it !in STOP_WORDS }

/** A crude stand-in for stemming: Polish inflection mostly changes the end of a word, so the first letters are kept. */
internal fun stemOf(token: String): String = token.take(STEM_LENGTH)

internal fun stemsOf(text: String): Set<String> = tokensOf(text).map(::stemOf).toSet()

internal fun areaLabel(area: SocialArea): String =
    when (area) {
        SocialArea.AGING -> "Starzenie się społeczeństwa"
        SocialArea.MENTAL_HEALTH -> "Zdrowie psychiczne"
        SocialArea.LONELINESS -> "Samotność i izolacja"
        SocialArea.DIGITAL_EXCLUSION -> "Wykluczenie cyfrowe"
        SocialArea.SERVICE_ACCESS -> "Dostęp do usług"
        SocialArea.COORDINATION -> "Koordynacja i współpraca"
        SocialArea.DEPOPULATION -> "Wyludnianie się regionów"
        SocialArea.OTHER -> "Inne"
    }

private fun stageSentence(stage: InnovationStage): String =
    when (stage) {
        InnovationStage.IDEA -> "To pomysł na wczesnym etapie, który można dopracować."
        InnovationStage.PILOT -> "Rozwiązanie sprawdzone w pilotażu."
        InnovationStage.TESTED -> "Rozwiązanie przetestowane w praktyce."
        InnovationStage.IMPLEMENTED -> "Rozwiązanie wdrożone i działające."
    }

internal class MatchHit(
    val innovation: InnovationDto,
    val relevance: Double,
    val matchedTerms: List<String>,
    val reasons: List<String>,
)

private class IndexedInnovation(
    val innovation: InnovationDto,
    /** Stem to the weight of the most telling place it occurs in. */
    val weights: Map<String, Double>,
)

private const val TITLE_WEIGHT = 3.0
private const val SUMMARY_WEIGHT = 2.0
private const val BODY_WEIGHT = 1.0
private const val REGION_BONUS = 1.5
private const val SATURATION = 8.0

/** A hit much weaker than the best one is a stray word in common, not an alternative. */
private const val RELATIVE_CUTOFF = 0.8
private const val MIN_RAW_SCORE = 2.5

/**
 * A small keyword search over the library: weighted overlap of word stems, rarer words counting more. It is a demo's
 * stand-in for the server's matching engine, good enough to return sensible innovations for a described problem.
 */
internal class MatchIndex(
    innovations: List<InnovationDto>,
    keywords: Map<String, List<String>>,
) {
    private val documents = innovations.map { index(it, keywords[it.id].orEmpty()) }

    private val documentFrequency: Map<String, Int> =
        documents.flatMap { it.weights.keys }.groupingBy { it }.eachCount()

    private fun index(
        innovation: InnovationDto,
        keywords: List<String>,
    ): IndexedInnovation {
        val weighted =
            listOf(
                stemsOf(innovation.title + " " + keywords.joinToString(" ")) to TITLE_WEIGHT,
                stemsOf(innovation.summary) to SUMMARY_WEIGHT,
                stemsOf(
                    listOfNotNull(innovation.description, innovation.problemDiagnosis, innovation.audienceDescription)
                        .joinToString(" "),
                ) to BODY_WEIGHT,
            )
        val weights =
            weighted
                .flatMap { (stems, weight) -> stems.map { it to weight } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, values) -> values.max() }
        return IndexedInnovation(innovation, weights)
    }

    private fun idf(stem: String): Double = ln(1.0 + documents.size / (1.0 + (documentFrequency[stem] ?: 0)))

    fun search(
        query: String,
        municipality: String? = null,
        limit: Int = DEFAULT_LIMIT,
    ): List<MatchHit> {
        val terms = tokensOf(query).distinctBy(::stemOf)
        val ranked =
            documents
                .mapNotNull { document -> hit(document, terms, municipality) }
                .sortedByDescending { it.relevance }
        val best = ranked.firstOrNull()?.relevance ?: return emptyList()
        return ranked.filter { it.relevance >= best * RELATIVE_CUTOFF }.take(limit)
    }

    private fun hit(
        document: IndexedInnovation,
        terms: List<String>,
        municipality: String?,
    ): MatchHit? {
        val matched = terms.filter { stemOf(it) in document.weights }
        val textual = matched.sumOf { idf(stemOf(it)) * document.weights.getValue(stemOf(it)) }
        val inRegion =
            municipality != null &&
                document.innovation.region
                    ?.contains(municipality.trim(), ignoreCase = true) == true
        val raw = textual + if (inRegion) REGION_BONUS else 0.0
        return if (textual < MIN_RAW_SCORE) {
            null
        } else {
            MatchHit(
                innovation = document.innovation,
                relevance = 1.0 - exp(-raw / SATURATION),
                matchedTerms = matched,
                reasons = reasonsFor(document.innovation, matched, inRegion),
            )
        }
    }

    private fun reasonsFor(
        innovation: InnovationDto,
        matched: List<String>,
        inRegion: Boolean,
    ): List<String> =
        buildList {
            add("Opis pasuje do Twojego problemu: ${matched.take(MAX_REASON_TERMS).joinToString(", ")}.")
            innovation.areas.firstOrNull()?.let { add("Obszar wyzwania: ${areaLabel(it)}.") }
            if (inRegion) add("Rozwiązanie działa w Twojej okolicy: ${innovation.region}.")
            add(stageSentence(innovation.stage))
        }

    companion object {
        const val DEFAULT_LIMIT = 5
        private const val MAX_REASON_TERMS = 4

        /** Below this relevance of the best hit, nothing in the library really fits. */
        const val GOOD_MATCH = 0.45
    }
}
