package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 2 (knowledge base) + module 4 (innovation tester).

/** `GET /api/innovations` (public) lists, `POST` (admin) creates. */
@Serializable
@Resource("innovations")
class Innovations(
    val parent: Api = Api(),
    val q: String? = null,
    val area: SocialArea? = null,
    val targetGroup: TargetGroup? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    /** `GET` (public), `PUT` / `DELETE` (admin). */
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Innovations = Innovations(),
        val id: String,
    ) {
        /**
         * `PUT` (authenticated): declare willingness to test the innovation. The caller has one such request per
         * innovation; repeating the call replaces its note until an admin decides on it. `GET` returns it (with its
         * status), `404` when there is none.
         */
        @Serializable
        @Resource("test-request")
        class TestRequest(
            val parent: ById,
        )

        /**
         * `PUT` (authenticated): rate the innovation and leave feedback; the caller's previous rating is replaced.
         * `GET` returns the caller's own rating, `404` when there is none.
         */
        @Serializable
        @Resource("feedback")
        class Feedback(
            val parent: ById,
        )
    }
}

@Serializable
data class InnovationSummary(
    val id: String,
    val title: String,
    val summary: String,
    val areas: List<SocialArea>,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
)

/**
 * An innovation of the library. [description] to [futureVision] mirror the narrative sections of ROPS' application
 * form (items 3 to 8): what the innovation is, what is new about it, the diagnosed problem, its audience, the change
 * it brings and its prospects. The applicant's personal data from the form is deliberately not part of the library.
 */
@Serializable
data class InnovationDto(
    val id: String,
    val title: String,
    val summary: String,
    /** Form item 3, "Opis innowacji". */
    val description: String,
    val areas: List<SocialArea>,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val region: String?,
    val mediaUrls: List<String>,
    val averageRating: Double?,
    val ratingsCount: Int,
    /** Form item 4, "Innowacyjność rozwiązania". */
    val innovativeness: String? = null,
    /** Form item 5, "Diagnoza problemu". */
    val problemDiagnosis: String? = null,
    /** Form item 6, "Opis odbiorców innowacji". */
    val audienceDescription: String? = null,
    /** Form item 7, "Zmiana, jaką wprowadza innowacja". */
    val expectedChange: String? = null,
    /** Form item 8, "Wizja przyszłości innowacji". */
    val futureVision: String? = null,
)

@Serializable
data class UpsertInnovationRequest(
    val title: String,
    val summary: String,
    val description: String,
    val areas: List<SocialArea>,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val region: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val innovativeness: String? = null,
    val problemDiagnosis: String? = null,
    val audienceDescription: String? = null,
    val expectedChange: String? = null,
    val futureVision: String? = null,
)

@Serializable
data class CreateTestRequest(
    val note: String? = null,
)

/** Outcome of a test request; only `NEW` ones can be decided, and a decision is final. */
@Serializable
enum class TestRequestStatus {
    NEW,
    ACCEPTED,
    DECLINED,
}

@Serializable
data class TestRequestDto(
    val id: String,
    val innovationId: String,
    val note: String?,
    val status: TestRequestStatus,
    val createdAt: String,
)

@Serializable
data class CreateFeedbackRequest(
    val rating: Int,
    val comment: String? = null,
    val suggestion: String? = null,
)

@Serializable
data class FeedbackDto(
    val id: String,
    val innovationId: String,
    val rating: Int,
    val comment: String?,
    val suggestion: String?,
    val createdAt: String,
)
