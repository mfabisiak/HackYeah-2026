package io.github.mfabisiak.hubmi.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Bm25IndexTest {
    private fun doc(
        key: String,
        title: List<String>,
        body: List<String> = emptyList(),
    ) = IndexInput(key, listOf(WeightedStems(title, 3.0), WeightedStems(body, 1.0)))

    private val index =
        Bm25Index.build(
            listOf(
                doc("seniors", title = listOf("senior", "sasiad"), body = listOf("pomoc", "zakupy", "senior")),
                doc("youth", title = listOf("mlodziez", "kryzys"), body = listOf("pomoc", "szkola")),
                doc("rural", title = listOf("wies", "hub"), body = listOf("pomoc", "swietlica")),
            ),
        )

    @Test
    fun documentsWithTheRareQueryTermRankFirst() {
        val hits = index.search(listOf("senior", "pomoc"))

        assertEquals("seniors", hits.first().key)
        assertEquals(setOf("senior", "pomoc"), hits.first().matchedStems)
    }

    @Test
    fun aTermInTheTitleOutweighsTheSameTermInTheBody() {
        val hits = index.search(listOf("kryzys", "zakupy"))

        assertEquals("youth", hits.first().key)
    }

    @Test
    fun scoresAreBetweenZeroAndOneAndDescending() {
        val hits = index.search(listOf("senior", "pomoc", "zakupy"))

        assertTrue(hits.all { it.score > 0.0 && it.score <= 1.0 })
        assertEquals(hits.map { it.score }.sortedDescending(), hits.map { it.score })
    }

    @Test
    fun unknownQueryTermsLowerTheRelevanceOfPartialMatches() {
        val exact = index.search(listOf("senior")).first().score
        val padded = index.search(listOf("senior", "samochod", "kryptowaluty")).first().score

        assertTrue(padded < exact, "foreign words should dilute the score ($padded vs $exact)")
    }

    @Test
    fun nothingMatchesAnEmptyOrForeignQuery() {
        assertTrue(index.search(emptyList()).isEmpty())
        assertTrue(index.search(listOf("samochod")).isEmpty())
        assertTrue(Bm25Index.build(emptyList<IndexInput<String>>()).search(listOf("senior")).isEmpty())
    }

    @Test
    fun searchDoesNotDependOnQueryTermOrderOrRepetition() {
        assertEquals(index.search(listOf("senior", "pomoc")), index.search(listOf("pomoc", "senior", "senior")))
    }
}
