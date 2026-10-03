package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 6 (admin panel). Everything under /api/admin requires the `admin` role.
// It also hosts the admin side of module 4 (innovation tester): feedback and test requests.

/** `GET`: aggregated needs by area and municipality, used to spot trends. */
@Serializable
@Resource("trends")
class AdminTrends(
    val parent: Api.Admin = Api.Admin(),
    val months: Int = 6,
)

/** `GET`: moderation queue. */
@Serializable
@Resource("ideas")
class AdminIdeas(
    val parent: Api.Admin = Api.Admin(),
    val status: IdeaStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    /** `PATCH`: change status and leave a comment for the author. */
    @Serializable
    @Resource("{id}/status")
    class Status(
        val parent: AdminIdeas = AdminIdeas(),
        val id: String,
    )
}

/** `GET`: grant applications list for admin. */
@Serializable
@Resource("applications")
class AdminApplications(
    val parent: Api.Admin = Api.Admin(),
    val callId: String? = null,
    val status: ApplicationStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
)

/** `GET`: ratings and comments left by testers, newest first; the texts may contain personal data. */
@Serializable
@Resource("feedback")
class AdminFeedback(
    val parent: Api.Admin = Api.Admin(),
    val innovationId: String? = null,
    val page: Int = 0,
    val size: Int = 20,
)

/** `GET`: declarations of willingness to test, newest first. */
@Serializable
@Resource("test-requests")
class AdminTestRequests(
    val parent: Api.Admin = Api.Admin(),
    val innovationId: String? = null,
    val status: TestRequestStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    /** `PATCH`: accept or decline a `NEW` request; any other transition is a `409`. */
    @Serializable
    @Resource("{id}/status")
    class Status(
        val parent: AdminTestRequests = AdminTestRequests(),
        val id: String,
    )
}

@Serializable
data class AdminFeedbackDto(
    val id: String,
    val innovationId: String,
    val innovationTitle: String,
    val userId: String,
    val rating: Int,
    val comment: String?,
    val suggestion: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class AdminTestRequestDto(
    val id: String,
    val innovationId: String,
    val innovationTitle: String,
    val userId: String,
    val note: String?,
    val status: TestRequestStatus,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class UpdateTestRequestStatusRequest(
    val status: TestRequestStatus,
)

@Serializable
data class AreaTrend(
    val area: SocialArea,
    val count: Int,
    val previousCount: Int,
)

@Serializable
data class MunicipalityTrend(
    val municipality: String,
    val count: Int,
)

@Serializable
data class MonthlyTrendPoint(
    val month: String,
    val count: Int,
)

@Serializable
data class TrendsDto(
    val byArea: List<AreaTrend>,
    val byMunicipality: List<MunicipalityTrend>,
    val unmatchedNeeds: Int,
    val series: List<MonthlyTrendPoint>,
    val topUnmatchedTerms: List<String>,
    /** Municipalities and words backed by fewer needs than this are left out of [byMunicipality]/[topUnmatchedTerms]. */
    val privacyThreshold: Int,
)

/** `GET`: admin dashboard counters. */
@Serializable
@Resource("summary")
class AdminSummary(
    val parent: Api.Admin = Api.Admin(),
)

@Serializable
data class AdminSummaryDto(
    val submittedIdeas: Int,
    val pendingTestRequests: Int,
    val unmatchedNeedsThisWeek: Int,
    val pendingThreads: Int,
)

@Serializable
data class UpdateIdeaStatusRequest(
    val status: IdeaStatus,
    val comment: String? = null,
)
