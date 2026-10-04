package io.github.mfabisiak.hubmi.web.adaptations

import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.StreamJs
import kotlin.js.Promise

/**
 * The Middleman (`/api/innovations/{id}/adaptations`, `/api/adaptations`); all calls need login. A plan is stored for
 * an admin to review, and its author can read it back with [mine] and [get].
 */
@JsExport
interface AdaptationsApi {
    /**
     * Asks the model how the innovation could be run by [institution]. Returns at once; `result` resolves to the stored
     * plan (or an `aiStatus` saying why there is none), and [listener] gets each step as the model writes it.
     */
    fun request(
        innovationId: String,
        institution: InstitutionJs,
        listener: AdaptationListenerJs? = null,
    ): StreamJs<AdaptationResponseJs>

    /** The caller's plans, newest first. */
    fun mine(
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<AdaptationJs>>>

    fun get(id: String): Promise<ApiResult<AdaptationJs>>
}
