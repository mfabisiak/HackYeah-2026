package io.github.mfabisiak.hubmi.web.innovations

import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import kotlin.js.Promise

/** `/api/innovations`: reading is public, create/update/delete need `admin`, test requests and feedback need login. */
@JsExport
interface InnovationsApi {
    fun list(
        q: String? = null,
        area: String? = null,
        targetGroup: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<InnovationSummaryJs>>>

    fun get(id: String): Promise<ApiResult<InnovationJs>>

    fun create(request: UpsertInnovationJs): Promise<ApiResult<InnovationJs>>

    fun update(
        id: String,
        request: UpsertInnovationJs,
    ): Promise<ApiResult<InnovationJs>>

    fun delete(id: String): Promise<ApiResult<EmptyJs>>

    /** Declares willingness to test the innovation; repeating it replaces the note until an admin decides. */
    fun requestTest(
        id: String,
        note: String? = null,
    ): Promise<ApiResult<TestRequestJs>>

    /** The caller's own test request for the innovation, with its status; an error with status 404 when there is none. */
    fun myTestRequest(id: String): Promise<ApiResult<TestRequestJs>>

    /** The caller's own rating of the innovation; an error with status 404 when there is none. */
    fun myFeedback(id: String): Promise<ApiResult<FeedbackJs>>

    /** Rates the innovation; the caller's previous rating is replaced. */
    fun sendFeedback(
        id: String,
        rating: Int,
        comment: String? = null,
        suggestion: String? = null,
    ): Promise<ApiResult<FeedbackJs>>
}
