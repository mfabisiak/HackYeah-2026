package io.github.mfabisiak.hubmi.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextAnalyzerTest {
    private val analyzer = TextAnalyzer(PrefixStemmer())

    @Test
    fun inflectedFormsShareAStem() {
        val stems = listOf("samotność", "samotnych", "samotnemu").flatMap(analyzer::stems)

        assertEquals(setOf("samot"), stems.toSet())
    }

    @Test
    fun diacriticsDoNotChangeTheStem() {
        assertEquals(analyzer.stems("Łódź żółć gęślą jaźń"), analyzer.stems("Lodz zolc gesla jazn"))
    }

    @Test
    fun stopWordsPunctuationAndOneLetterWordsAreDropped() {
        val tokens = analyzer.analyze("Mam w domu starszą mamę, a ona nie wychodzi z domu!")

        assertEquals(listOf("domu", "starszą", "mamę", "wychodzi", "domu"), tokens.map(Token::surface))
    }

    @Test
    fun surfaceFormKeepsWhatTheUserTypedInLowerCase() {
        assertEquals(listOf(Token("alzheimerem", "alzhe")), analyzer.analyze("Alzheimerem"))
    }

    @Test
    fun emptyAndSymbolOnlyTextHaveNoTokens() {
        assertTrue(analyzer.analyze("").isEmpty())
        assertTrue(analyzer.analyze("?! ... -- 1").isEmpty())
    }
}
