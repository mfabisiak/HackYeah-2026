package io.github.mfabisiak.hubmi.web.ideas

import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import kotlin.js.Promise

/** `/api/ideas`: all calls need login. */
@JsExport
interface IdeasApi {
    fun create(request: CreateIdeaJs): Promise<ApiResult<IdeaJs>>

    /** Ideas submitted by the caller. */
    fun mine(
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<IdeaJs>>>

    fun get(id: String): Promise<ApiResult<IdeaJs>>
}

/** `/api/calls`: reading is public, create/update/delete need `admin`, applying needs login. */
@JsExport
interface CallsApi {
    /** @param status `CallStatus` name to filter by. */
    fun list(status: String? = null): Promise<ApiResult<Array<GrantCallJs>>>

    /** Calls open right now. */
    fun active(): Promise<ApiResult<Array<GrantCallJs>>>

    fun get(id: String): Promise<ApiResult<GrantCallJs>>

    /**
     * Declarations and RODO clauses of a call.
     *
     * @param applicantType `ApplicantType` name; without it the clauses of every applicant variant are returned.
     */
    fun declarations(
        id: String,
        applicantType: String? = null,
    ): Promise<ApiResult<DeclarationsJs>>

    fun create(request: UpsertCallJs): Promise<ApiResult<GrantCallJs>>

    fun update(
        id: String,
        request: UpsertCallJs,
    ): Promise<ApiResult<GrantCallJs>>

    fun delete(id: String): Promise<ApiResult<EmptyJs>>

    /** Creates a draft application generated from the call's template. */
    fun apply(
        callId: String,
        ideaId: String? = null,
    ): Promise<ApiResult<ApplicationJs>>
}
