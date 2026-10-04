package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.AdaptationPlanDto
import kotlinx.serialization.Serializable

/** How an innovation could be run by an institution; the steps are in the order they start. */
@Serializable
data class AdaptationPlan(
    val serviceForm: String,
    val steps: List<AdaptationStep>,
    val requiredResources: List<String>,
    val risks: List<String>,
    val estimatedCostPln: Int,
    val exceedsBudget: Boolean,
)

fun AdaptationPlan.toDto(): AdaptationPlanDto =
    AdaptationPlanDto(
        serviceForm = serviceForm,
        steps = steps.map(AdaptationStep::toDto),
        requiredResources = requiredResources,
        risks = risks,
        estimatedCostPln = estimatedCostPln,
        exceedsBudget = exceedsBudget,
    )
