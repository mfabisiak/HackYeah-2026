package io.github.mfabisiak.hubmi.service.matching

fun interface Stemmer {
    fun stem(word: String): String
}
