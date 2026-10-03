package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.common.mongo.matches

/** Best matches, most relevant first; [noGoodMatch] is true exactly when no innovation passed the threshold. */
data class EngineResult(
    val matches: List<ScoredInnovation>,
) {
    val noGoodMatch: Boolean get() = matches.isEmpty()
}
