package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.Serializable

/**
 * Shared building blocks of the contract.
 *
 * Conventions: identifiers are plain strings (hex ids), timestamps are ISO-8601 strings; this keeps the contract
 * trivially exportable to JS. Server-side domain code wraps ids in value classes.
 */
@Serializable
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Int,
)

@Serializable
data class PageRequest(
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) {
    val skip: Int get() = page * size
    val limit: Int get() = size

    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100
    }
}

/** Thematic areas of social challenges (taxonomy based on the Social Challenges Map). */
@Serializable
enum class SocialArea {
    AGING,
    MENTAL_HEALTH,
    LONELINESS,
    DIGITAL_EXCLUSION,
    SERVICE_ACCESS,
    COORDINATION,
    DEPOPULATION,
    OTHER,
}

@Serializable
enum class TargetGroup {
    SENIORS,
    YOUTH,
    PEOPLE_WITH_DISABILITIES,
    FAMILIES,
    RESIDENTS,
    NGOS,
    LOCAL_GOVERNMENTS,
}

@Serializable
enum class InnovationStage {
    IDEA,
    PILOT,
    TESTED,
    IMPLEMENTED,
}
