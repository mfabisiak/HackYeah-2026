package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 6 (admin panel). Everything under /api/admin requires the `admin` role.

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
data class TrendsDto(
    val byArea: List<AreaTrend>,
    val byMunicipality: List<MunicipalityTrend>,
    val unmatchedNeeds: Int,
)

@Serializable
data class UpdateIdeaStatusRequest(
    val status: IdeaStatus,
    val comment: String? = null,
)
