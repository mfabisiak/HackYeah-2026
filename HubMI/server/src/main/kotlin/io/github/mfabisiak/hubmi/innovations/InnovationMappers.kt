package io.github.mfabisiak.hubmi.innovations

import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.common.HttpUrl

fun InnovationItem.toSummary(): InnovationSummary =
    InnovationSummary(
        id = id.toHexString(),
        title = title,
        summary = summary,
        areas = areas,
        targetGroups = targetGroups,
        stage = stage,
    )

fun InnovationItem.toDto(): InnovationDto =
    InnovationDto(
        id = id.toHexString(),
        title = title,
        summary = summary,
        description = description,
        areas = areas,
        targetGroups = targetGroups,
        stage = stage,
        region = region,
        mediaUrls = mediaUrls,
        averageRating = if (ratingsCount > 0) ratingSum.toDouble() / ratingsCount else null,
        ratingsCount = ratingsCount,
    )

fun InnovationDraft.toItem(now: String): InnovationItem =
    InnovationItem(
        title = title.value,
        summary = summary.value,
        description = description.value,
        areas = areas.toList(),
        targetGroups = targetGroups.toList(),
        stage = stage,
        region = region?.value,
        mediaUrls = mediaUrls.map(HttpUrl::value),
        createdAt = now,
        updatedAt = now,
    )
