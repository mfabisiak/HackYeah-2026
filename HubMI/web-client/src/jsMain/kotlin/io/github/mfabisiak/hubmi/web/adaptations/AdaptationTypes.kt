package io.github.mfabisiak.hubmi.web.adaptations

import arrow.core.Either
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationPlanDto
import io.github.mfabisiak.hubmi.api.AdaptationResponse
import io.github.mfabisiak.hubmi.api.AdaptationStepDto
import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.InstitutionType
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.enumOf

/** The institution the innovation is adapted to; also what the plan was written for. */
@JsExport
class InstitutionJs(
    /** `InstitutionType` name: `LOCAL_GOVERNMENT`, `NGO`, `SOCIAL_SERVICES_CENTER` or `OTHER`. */
    val type: String,
    /** People the institution can assign to the service. */
    val staffCount: Int,
    /** What the institution can spend on a pilot, in PLN. */
    val budgetPln: Int,
    /** Local conditions in the caller's words. */
    val context: String,
)

@JsExport
class AdaptationStepJs(
    val title: String,
    val description: String,
    /** Month of the pilot in which the step starts, `0` being its first. */
    val startMonth: Int,
)

@JsExport
class AdaptationPlanJs(
    val serviceForm: String,
    val steps: Array<AdaptationStepJs>,
    val requiredResources: Array<String>,
    val risks: Array<String>,
    val estimatedCostPln: Int,
    /** True when the estimate is above the institution's budget: the plan needs trimming. */
    val exceedsBudget: Boolean,
)

@JsExport
class AdaptationJs(
    val id: String,
    val innovationId: String,
    val innovationTitle: String,
    val institution: InstitutionJs,
    val plan: AdaptationPlanJs,
    /** Always true: show the plan as written by AI until an admin has reviewed it. */
    val aiGenerated: Boolean,
    /** The model that wrote the plan. */
    val model: String,
    /** `AdaptationStatus` name: `PENDING_REVIEW`, `APPROVED` or `REJECTED`. */
    val status: String,
    val adminComment: String?,
    val createdAt: String,
)

/** What asking for a plan comes to: [adaptation] is there exactly when [aiStatus] is `OK`. */
@JsExport
class AdaptationResponseJs(
    /** `AiStatus` name: `OK`, `UNAVAILABLE` or `INVALID_OUTPUT`. */
    val aiStatus: String,
    val adaptation: AdaptationJs?,
)

/** Callback for the steps of the plan as the model writes them; a throwing callback is logged, not fatal. */
@JsExport
external interface AdaptationListenerJs {
    val onStep: ((AdaptationStepJs) -> Unit)?
        get() = definedExternally
}

internal fun InstitutionJs.toDto(): Either<ApiErrorJs, InstitutionProfile> =
    either {
        InstitutionProfile(enumOf<InstitutionType>(type, "type"), staffCount, budgetPln, context)
    }

private fun InstitutionProfile.toJs(): InstitutionJs = InstitutionJs(type.name, staffCount, budgetPln, context)

internal fun AdaptationStepDto.toJs(): AdaptationStepJs = AdaptationStepJs(title, description, startMonth)

private fun AdaptationPlanDto.toJs(): AdaptationPlanJs =
    AdaptationPlanJs(
        serviceForm = serviceForm,
        steps = steps.map { it.toJs() }.toTypedArray(),
        requiredResources = requiredResources.toTypedArray(),
        risks = risks.toTypedArray(),
        estimatedCostPln = estimatedCostPln,
        exceedsBudget = exceedsBudget,
    )

internal fun AdaptationDto.toJs(): AdaptationJs =
    AdaptationJs(
        id = id,
        innovationId = innovationId,
        innovationTitle = innovationTitle,
        institution = institution.toJs(),
        plan = plan.toJs(),
        aiGenerated = aiGenerated,
        model = model,
        status = status.name,
        adminComment = adminComment,
        createdAt = createdAt,
    )

internal fun AdaptationResponse.toJs(): AdaptationResponseJs = AdaptationResponseJs(aiStatus.name, adaptation?.toJs())
