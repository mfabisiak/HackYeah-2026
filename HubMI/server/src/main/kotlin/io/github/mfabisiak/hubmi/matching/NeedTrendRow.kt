package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.api.SocialArea
import kotlinx.serialization.Serializable

/** The part of a need the trends are computed from; the text is left out so aggregating does not load it. */
@Serializable
data class NeedTrendRow(
    val areas: List<SocialArea> = emptyList(),
    val municipality: String? = null,
    val createdAt: String,
)
