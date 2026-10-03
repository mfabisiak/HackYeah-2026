package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 3 (idea creator) and grant calls (application generator).

/** `POST` (authenticated): submit an idea card. */
@Serializable
@Resource("ideas")
class Ideas(
    val parent: Api = Api(),
) {
    /** `GET` (authenticated): ideas submitted by the caller. */
    @Serializable
    @Resource("mine")
    class Mine(
        val parent: Ideas = Ideas(),
        val page: Int = 0,
        val size: Int = 20,
    )

    /** `GET` (authenticated, author or admin). */
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Ideas = Ideas(),
        val id: String,
    )
}

/** `GET` (public): calls for proposals; admin manages them with `POST`, `PUT`, `DELETE`. */
@Serializable
@Resource("calls")
class Calls(
    val parent: Api = Api(),
    val status: CallStatus? = null,
) {
    /** `GET` (public): calls open right now. */
    @Serializable
    @Resource("active")
    class Active(
        val parent: Calls = Calls(),
    )

    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Calls = Calls(),
        val id: String,
    ) {
        /** `POST` (authenticated): submit an application generated from the call's template. */
        @Serializable
        @Resource("applications")
        class Applications(
            val parent: ById,
        )

        /** `GET`: declarations and RODO clauses for this call. */
        @Serializable
        @Resource("declarations")
        class Declarations(
            val parent: ById,
            val applicantType: ApplicantType? = null,
        )
    }
}

@Serializable
enum class IdeaStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    ACCEPTED,
    REJECTED,
}

@Serializable
enum class CallStatus {
    UPCOMING,
    OPEN,
    CLOSED,
}

@Serializable
data class CreateIdeaRequest(
    val title: String,
    /** What the idea is about, in a nutshell. */
    val essence: String,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
)

@Serializable
data class IdeaDto(
    val id: String,
    val title: String,
    val essence: String,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val status: IdeaStatus,
    val adminComment: String?,
    val createdAt: String,
)

@Serializable
data class CallField(
    val key: String,
    val label: String,
    val required: Boolean,
)

@Serializable
data class GrantCallDto(
    val id: String,
    val title: String,
    val description: String,
    val opensAt: String,
    val closesAt: String,
    val status: CallStatus,
    /** Form template the application generator renders for this call. */
    val fields: List<CallField>,
)

@Serializable
data class UpsertCallRequest(
    val title: String,
    val description: String,
    val opensAt: String,
    val closesAt: String,
    val fields: List<CallField>,
)
