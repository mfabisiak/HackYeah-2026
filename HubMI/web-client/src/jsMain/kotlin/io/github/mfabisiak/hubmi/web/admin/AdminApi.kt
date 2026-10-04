package io.github.mfabisiak.hubmi.web.admin

import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.adaptations.AdaptationJs
import io.github.mfabisiak.hubmi.web.ideas.ApplicationJs
import io.github.mfabisiak.hubmi.web.ideas.IdeaJs
import kotlin.js.Promise

/** `/api/admin`: every call needs the `admin` role. */
@JsExport
interface AdminApi {
    /** Aggregated needs by area and municipality over the last [months] months. */
    fun trends(months: Int = 6): Promise<ApiResult<TrendsJs>>

    /** Admin dashboard counters. */
    fun summary(): Promise<ApiResult<AdminSummaryJs>>

    /** Moderation queue. @param status `IdeaStatus` name to filter by. */
    fun ideas(
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<IdeaJs>>>

    /** Changes the status of an idea and leaves a comment for its author. */
    fun updateIdeaStatus(
        id: String,
        status: String,
        comment: String? = null,
    ): Promise<ApiResult<IdeaJs>>

    /** Grant applications list for admin. */
    fun applications(
        callId: String? = null,
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ApplicationJs>>>

    /** Ratings and comments of testers, newest first; optionally for one innovation. */
    fun feedback(
        innovationId: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<AdminFeedbackJs>>>

    /** Declarations of willingness to test. @param status `TestRequestStatus` name to filter by. */
    fun testRequests(
        innovationId: String? = null,
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<AdminTestRequestJs>>>

    /** Accepts or declines a `NEW` test request (`ACCEPTED` or `DECLINED`); a decision is final. */
    fun decideTestRequest(
        id: String,
        status: String,
    ): Promise<ApiResult<AdminTestRequestJs>>

    /** Plans of the Middleman waiting for review, newest first. @param status `AdaptationStatus` name to filter by. */
    fun adaptations(
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<AdaptationJs>>>

    /** Approves or rejects a `PENDING_REVIEW` plan; a decision is final, and rejecting needs a [comment]. */
    fun reviewAdaptation(
        id: String,
        status: String,
        comment: String? = null,
    ): Promise<ApiResult<AdaptationJs>>
}
