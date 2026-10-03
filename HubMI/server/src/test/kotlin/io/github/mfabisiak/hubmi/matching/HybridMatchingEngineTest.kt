package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.Catalogue
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.garden
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.loneliness
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.transport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HybridMatchingEngineTest {
    private val catalogue = Catalogue(listOf(transport, garden, loneliness))
    private val engines = HybridTestFixtures.engines(catalogue)

    private fun match(
        query: String,
        municipality: MunicipalityName? = null,
    ): EngineResult =
        assertIs<Either.Right<EngineResult>>(runBlocking { engines.hybrid.match(query, municipality) }).value

    @Test
    fun findsAnInnovationThatSharesMeaningButNoWordWithTheQuery() {
        val query = "Brak jak dojechać do lekarza"

        val hybrid = match(query)
        val keyword = assertIs<Either.Right<EngineResult>>(runBlocking { engines.keyword.match(query, null) }).value

        assertEquals(
            transport.id,
            hybrid.matches
                .first()
                .innovation.id,
        )
        assertTrue(keyword.noGoodMatch, "the keyword engine cannot see the connection, which is why the hybrid exists")
    }

    @Test
    fun aSemanticOnlyMatchIsExplainedAsSuchAndListsNoPhrases() {
        val best = match("Brak jak dojechać do lekarza").matches.first()

        assertTrue(best.matchedTerms.isEmpty())
        assertTrue("Zbliżony znaczeniowo do opisu problemu." in best.reasons, best.reasons.toString())
    }

    @Test
    fun sharedWordsStillCountAndAreReported() {
        val best = match("samotnych seniorów potrzebują rozmowy").matches.first()

        assertEquals(loneliness.id, best.innovation.id)
        assertTrue(best.matchedTerms.isNotEmpty())
    }

    @Test
    fun anUnrelatedQueryHasNoGoodMatch() {
        assertTrue(match("kupno używanego samochodu").noGoodMatch)
    }

    @Test
    fun scoresAreRelevanceBetweenZeroAndOne() {
        val matches = match("Brak jak dojechać do lekarza").matches

        assertTrue(matches.all { it.score in 0.0..1.0 })
        assertEquals(matches.map { it.score }.sortedDescending(), matches.map { it.score })
    }

    @Test
    fun theMunicipalityBoostsInnovationsFromThatRegion() {
        val local =
            HybridTestFixtures
                .innovation(
                    "Transport dla wsi",
                    "Dowóz do przychodni",
                ).copy(region = "Powiat tatrzański")
        val elsewhere =
            HybridTestFixtures
                .innovation(
                    "Transport miejski",
                    "Dowóz do przychodni",
                ).copy(region = "Kraków")
        val regional = HybridTestFixtures.engines(Catalogue(listOf(elsewhere, local)))
        val query = "Brak jak dojechać do lekarza"
        val municipality = MunicipalityName.parse("tatrzański", "municipality").getOrNull()

        val result =
            assertIs<Either.Right<EngineResult>>(runBlocking { regional.hybrid.match(query, municipality) }).value

        assertEquals(
            local.id,
            result.matches
                .first()
                .innovation.id,
        )
    }

    @Test
    fun anUnavailableEmbedderIsAnUnavailableError() {
        engines.embedder.down.set(true)

        val result = runBlocking { engines.hybrid.match("dojazd do lekarza", null) }

        assertIs<DomainError.Unavailable>(result.leftOrNull())
    }

    @Test
    fun warmUpEmbedsTheCatalogueOnceSoTheFirstMatchOnlyEmbedsTheQuery() {
        assertIs<Either.Right<Unit>>(runBlocking { engines.hybrid.warmUp() })
        val embeddedAfterWarmUp = engines.embedder.embeddedTexts.get()

        match("dojazd do lekarza")

        assertEquals(3, embeddedAfterWarmUp)
        assertEquals(embeddedAfterWarmUp + 1, engines.embedder.embeddedTexts.get())
    }

    @Test
    fun warmUpReportsAnUnavailableEmbedder() {
        engines.embedder.down.set(true)

        assertIs<DomainError.Unavailable>(runBlocking { engines.hybrid.warmUp() }.leftOrNull())
    }
}
