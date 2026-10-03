package io.github.mfabisiak.hubmi.web.admin

import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminSummaryDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.MonthlyTrendPoint
import io.github.mfabisiak.hubmi.api.TrendsDto

@JsExport
class AreaTrendJs(
    /** `SocialArea` name. */
    val area: String,
    val count: Int,
    val previousCount: Int,
)

@JsExport
class MunicipalityTrendJs(
    val municipality: String,
    val count: Int,
)

@JsExport
class MonthlyTrendPointJs(
    val month: String,
    val count: Int,
)

@JsExport
class TrendsJs(
    val byArea: Array<AreaTrendJs>,
    val byMunicipality: Array<MunicipalityTrendJs>,
    val unmatchedNeeds: Int,
    val series: Array<MonthlyTrendPointJs>,
    val topUnmatchedTerms: Array<String>,
    val privacyThreshold: Int,
)

internal fun MonthlyTrendPoint.toJs(): MonthlyTrendPointJs = MonthlyTrendPointJs(month, count)

internal fun TrendsDto.toJs(): TrendsJs =
    TrendsJs(
        byArea = byArea.map { AreaTrendJs(it.area.name, it.count, it.previousCount) }.toTypedArray(),
        byMunicipality = byMunicipality.map { MunicipalityTrendJs(it.municipality, it.count) }.toTypedArray(),
        unmatchedNeeds = unmatchedNeeds,
        series = series.map { it.toJs() }.toTypedArray(),
        topUnmatchedTerms = topUnmatchedTerms.toTypedArray(),
        privacyThreshold = privacyThreshold,
    )

@JsExport
class AdminSummaryJs(
    val submittedIdeas: Int,
    val pendingTestRequests: Int,
    val unmatchedNeedsThisWeek: Int,
    val pendingThreads: Int,
)

internal fun AdminSummaryDto.toJs(): AdminSummaryJs =
    AdminSummaryJs(
        submittedIdeas = submittedIdeas,
        pendingTestRequests = pendingTestRequests,
        unmatchedNeedsThisWeek = unmatchedNeedsThisWeek,
        pendingThreads = pendingThreads,
    )

@JsExport
class AdminFeedbackJs(
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

@JsExport
class AdminTestRequestJs(
    val id: String,
    val innovationId: String,
    val innovationTitle: String,
    val userId: String,
    val note: String?,
    /** `TestRequestStatus` name. */
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

internal fun AdminFeedbackDto.toJs(): AdminFeedbackJs =
    AdminFeedbackJs(id, innovationId, innovationTitle, userId, rating, comment, suggestion, createdAt, updatedAt)

internal fun AdminTestRequestDto.toJs(): AdminTestRequestJs =
    AdminTestRequestJs(id, innovationId, innovationTitle, userId, note, status.name, createdAt, updatedAt)
