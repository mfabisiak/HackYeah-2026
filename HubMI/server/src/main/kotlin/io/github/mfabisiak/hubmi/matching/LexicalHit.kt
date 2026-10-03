package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.innovations.InnovationItem

/** An innovation found by the text index; [score] is the BM25 relevance in `0..1` before any region boost. */
data class LexicalHit(
    val innovation: InnovationItem,
    val score: Double,
    val matchedTerms: List<String>,
)
