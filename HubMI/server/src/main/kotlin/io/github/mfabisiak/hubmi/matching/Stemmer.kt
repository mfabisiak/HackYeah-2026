package io.github.mfabisiak.hubmi.matching

fun interface Stemmer {
    fun stem(word: String): String
}
