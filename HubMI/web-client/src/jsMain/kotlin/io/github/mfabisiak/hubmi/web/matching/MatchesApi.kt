package io.github.mfabisiak.hubmi.web.matching

import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import kotlin.js.Promise

/** `/api/matches`: public, login optional. */
@JsExport
interface MatchesApi {
    /** Describes a problem and returns matching innovations. */
    fun match(
        description: String,
        municipality: String? = null,
    ): Promise<ApiResult<MatchResultJs>>

    /** "Did this help?" for the need created by [match]. */
    fun sendFeedback(
        needId: String,
        helpful: Boolean,
    ): Promise<ApiResult<EmptyJs>>
}
