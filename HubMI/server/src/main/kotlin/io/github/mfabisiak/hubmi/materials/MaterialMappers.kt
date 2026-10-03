package io.github.mfabisiak.hubmi.materials

import io.github.mfabisiak.hubmi.api.MaterialDto

fun MaterialItem.toDto(): MaterialDto =
    MaterialDto(
        id = id.toHexString(),
        title = title,
        description = description,
        type = type,
        url = url,
        areas = areas,
    )

fun MaterialDraft.toItem(now: String): MaterialItem =
    MaterialItem(
        title = title.value,
        description = description.value,
        type = type,
        url = url.value,
        areas = areas.toList(),
        createdAt = now,
        updatedAt = now,
    )
