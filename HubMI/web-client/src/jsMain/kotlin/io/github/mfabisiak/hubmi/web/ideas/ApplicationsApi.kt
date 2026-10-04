package io.github.mfabisiak.hubmi.web.ideas

import io.github.mfabisiak.hubmi.api.Applications
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import kotlin.js.Promise

@JsExport
interface ApplicationsApi {
    /** Applications submitted or drafted by the caller. */
    fun mine(
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ApplicationJs>>>

    fun get(id: String): Promise<ApiResult<ApplicationJs>>

    /** Creates a draft application for the given call (optionally prefilled from idea). */
    fun createDraft(
        callId: String,
        ideaId: String? = null,
    ): Promise<ApiResult<ApplicationJs>>

    /** Saves a draft application by passing a raw JSON string of [SaveApplicationDraftRequest]. */
    fun saveDraft(
        id: String,
        bodyJson: String,
    ): Promise<ApiResult<ApplicationJs>>

    /** Submits a draft application. */
    fun submit(id: String): Promise<ApiResult<ApplicationJs>>
}
