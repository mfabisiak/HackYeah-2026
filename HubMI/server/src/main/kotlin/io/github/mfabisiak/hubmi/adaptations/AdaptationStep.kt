package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.AdaptationStepDto
import kotlinx.serialization.Serializable

@Serializable
data class AdaptationStep(
    val title: String,
    val description: String,
    val startMonth: Int,
)

fun AdaptationStep.toDto(): AdaptationStepDto =
    AdaptationStepDto(title = title, description = description, startMonth = startMonth)
