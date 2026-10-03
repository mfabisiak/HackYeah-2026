package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 1 (social matchmaking) – the mandatory module, see docs/MATCHMAKING.md.

/** `POST` (public, login optional): describe a problem, get matching innovations. */
@Serializable
@Resource("matches")
class Matches(
    val parent: Api = Api(),
) {
    /** `PUT` (public): "did this help?" for the need created by a match request; replaces the previous answer. */
    @Serializable
    @Resource("{needId}/feedback")
    class Feedback(
        val parent: Matches = Matches(),
        val needId: String,
    )
}

@Serializable
data class MatchRequest(
    val description: String,
    val municipality: String? = null,
)

@Serializable
data class MatchDto(
    val innovation: InnovationSummary,
    val score: Double,
    /** Human-readable explanation, one sentence per reason. */
    val reasons: List<String>,
    val matchedTerms: List<String>,
)

@Serializable
data class SimilarNeedDto(
    val id: String,
    val excerpt: String,
    val area: SocialArea?,
)

@Serializable
data class MatchResult(
    val needId: String,
    val matches: List<MatchDto>,
    val similarNeeds: List<SimilarNeedDto>,
    /** True when nothing passed the relevance threshold; the client should suggest submitting an idea. */
    val noGoodMatch: Boolean,
)

@Serializable
data class MatchFeedbackRequest(
    val helpful: Boolean,
)
