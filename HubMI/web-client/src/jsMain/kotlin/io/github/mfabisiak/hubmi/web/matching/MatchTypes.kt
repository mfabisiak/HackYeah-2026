package io.github.mfabisiak.hubmi.web.matching

import io.github.mfabisiak.hubmi.api.MatchDto
import io.github.mfabisiak.hubmi.api.MatchResult
import io.github.mfabisiak.hubmi.api.SimilarNeedDto
import io.github.mfabisiak.hubmi.web.innovations.InnovationSummaryJs
import io.github.mfabisiak.hubmi.web.innovations.toJs

@JsExport
class MatchJs(
    val innovation: InnovationSummaryJs,
    val score: Double,
    /** Human-readable explanation, one sentence per reason. */
    val reasons: Array<String>,
    val matchedTerms: Array<String>,
)

@JsExport
class SimilarNeedJs(
    val id: String,
    val excerpt: String,
    /** `SocialArea` name, if known. */
    val area: String?,
)

@JsExport
class MatchResultJs(
    val needId: String,
    val matches: Array<MatchJs>,
    val similarNeeds: Array<SimilarNeedJs>,
    /** True when nothing passed the relevance threshold; suggest submitting an idea. */
    val noGoodMatch: Boolean,
)

internal fun MatchResult.toJs(): MatchResultJs =
    MatchResultJs(
        needId = needId,
        matches = matches.map { it.toJs() }.toTypedArray(),
        similarNeeds = similarNeeds.map { it.toJs() }.toTypedArray(),
        noGoodMatch = noGoodMatch,
    )

private fun MatchDto.toJs(): MatchJs =
    MatchJs(innovation.toJs(), score, reasons.toTypedArray(), matchedTerms.toTypedArray())

private fun SimilarNeedDto.toJs(): SimilarNeedJs = SimilarNeedJs(id, excerpt, area?.name)
