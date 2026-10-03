package io.github.mfabisiak.hubmi.matching

import org.apache.lucene.analysis.pl.PolishAnalyzer
import org.apache.lucene.analysis.stempel.StempelStemmer

/** Lucene's Stempel (table-driven) Polish stemmer; the shared trie is immutable, so one instance serves all threads. */
class StempelPolishStemmer : Stemmer {
    override fun stem(word: String): String =
        StempelStemmer(table)
            .stem(word)
            ?.takeIf { it.isNotEmpty() }
            ?.toString()
            ?: word

    private companion object {
        val table = PolishAnalyzer.getDefaultTable()
    }
}
