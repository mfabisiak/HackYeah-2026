package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.common.mongo.matches

/** Crude but dependency-free Polish stemming: keep the first [length] characters (inflection lives in the suffix). */
class PrefixStemmer(
    private val length: Int = DEFAULT_LENGTH,
) : Stemmer {
    override fun stem(word: String): String = word.take(length)

    companion object {
        /** Chosen on the golden set, where it matches Lucene's Stempel without the dependency (docs/matching-baseline.md). */
        const val DEFAULT_LENGTH = 5
    }
}
