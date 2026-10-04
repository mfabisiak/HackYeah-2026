package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.AdaptationDto

fun AdaptationItem.toDto(innovationTitle: String): AdaptationDto =
    AdaptationDto(
        id = id.toHexString(),
        innovationId = innovationId.toHexString(),
        innovationTitle = innovationTitle,
        institution = institution.toDto(),
        plan = plan.toDto(),
        model = model,
        status = status,
        adminComment = adminComment,
        createdAt = createdAt,
    )
