package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Module 7 (Middleman of innovations): a plan to run a library innovation as a service of a given institution, written
// by a local model and reviewed by an admin, see docs/ASSISTANT.md. `POST /api/innovations/{id}/adaptations` is in
// Innovations.kt, the review queue in Admin.kt.

/** `GET` (authenticated): the caller's own plans, newest first. */
@Serializable
@Resource("adaptations")
class Adaptations(
    val parent: Api = Api(),
) {
    @Serializable
    @Resource("mine")
    class Mine(
        val parent: Adaptations = Adaptations(),
        val page: Int = 0,
        val size: Int = 20,
    )

    /** `GET` (authenticated, author or admin). */
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Adaptations = Adaptations(),
        val id: String,
    )
}

@Serializable
enum class InstitutionType {
    /** A municipality or other local government (JST). */
    LOCAL_GOVERNMENT,
    NGO,

    /** A social services centre (CUS). */
    SOCIAL_SERVICES_CENTER,
    OTHER,
}

/** The institution the innovation is to be adapted to: what it has, and what it can spend on a pilot. */
@Serializable
data class InstitutionProfile(
    val type: InstitutionType,
    /** People the institution can assign to the service. */
    val staffCount: Int,
    /** What the institution can spend on a pilot, in PLN. */
    val budgetPln: Int,
    /** Local conditions in the caller's words: area, population, what already exists, what is missing. */
    val context: String,
)

@Serializable
data class AdaptationStepDto(
    val title: String,
    val description: String,
    /** Month of the pilot in which the step starts, `0` being its first. */
    val startMonth: Int,
)

@Serializable
data class AdaptationPlanDto(
    /** The shape of the service the institution would run. */
    val serviceForm: String,
    val steps: List<AdaptationStepDto>,
    val requiredResources: List<String>,
    val risks: List<String>,
    val estimatedCostPln: Int,
    /** True when the model's estimate is above the budget of the institution: the plan needs trimming. */
    val exceedsBudget: Boolean,
)

@Serializable
enum class AdaptationStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
}

@Serializable
data class AdaptationDto(
    val id: String,
    val innovationId: String,
    val innovationTitle: String,
    val institution: InstitutionProfile,
    val plan: AdaptationPlanDto,
    /** Always true: the plan is a proposal of a model, to be shown as such until an admin has reviewed it. */
    val aiGenerated: Boolean = true,
    /** The model that wrote the plan. */
    val model: String,
    val status: AdaptationStatus,
    val adminComment: String?,
    val createdAt: String,
)

/** The outcome of asking for a plan: [adaptation] is there exactly when [aiStatus] is [AiStatus.OK]. */
@Serializable
data class AdaptationResponse(
    val aiStatus: AiStatus,
    val adaptation: AdaptationDto?,
)

@Serializable
data class UpdateAdaptationStatusRequest(
    val status: AdaptationStatus,
    /** For the author; required when rejecting. */
    val comment: String? = null,
)

/**
 * One step of a plan streamed as server-sent events (`event:` is the [SerialName] of the case, `data:` the JSON of the
 * event). The stream ends with [Done], or with [Failed] when the plan was written but could not be stored.
 */
@Serializable
sealed interface AdaptationEvent {
    /** A step of the plan, sent as soon as the model has written it; the plan that is stored may drop or reorder some. */
    @Serializable
    @SerialName(AdaptationEvent.STEP)
    data class Step(
        val step: AdaptationStepDto,
    ) : AdaptationEvent

    @Serializable
    @SerialName(AdaptationEvent.DONE)
    data class Done(
        val response: AdaptationResponse,
    ) : AdaptationEvent

    /** A server fault (the database); the plan was not stored. */
    @Serializable
    @SerialName(AdaptationEvent.FAILED)
    data class Failed(
        val error: ErrorResponse,
    ) : AdaptationEvent

    companion object {
        const val STEP = "step"
        const val DONE = "done"
        const val FAILED = "failed"
    }
}
