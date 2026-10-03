package io.github.mfabisiak.hubmi.matching

/** A word of the analysed text: [surface] as typed (lower-cased) and [stem] used for matching. */
data class Token(
    val surface: String,
    val stem: String,
)
