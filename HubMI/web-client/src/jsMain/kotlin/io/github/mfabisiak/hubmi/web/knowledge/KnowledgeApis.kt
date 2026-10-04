package io.github.mfabisiak.hubmi.web.knowledge

import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import kotlin.js.Promise

/** `/api/challenges`: reading is public, create/update/delete need `admin`. */
@JsExport
interface ChallengesApi {
    fun list(
        area: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ChallengeJs>>>

    fun get(id: String): Promise<ApiResult<ChallengeJs>>

    fun create(request: UpsertChallengeJs): Promise<ApiResult<ChallengeJs>>

    fun update(
        id: String,
        request: UpsertChallengeJs,
    ): Promise<ApiResult<ChallengeJs>>

    fun delete(id: String): Promise<ApiResult<EmptyJs>>
}

/** `/api/materials`: reading is public, create/update/delete need `admin`. */
@JsExport
interface MaterialsApi {
    fun list(
        q: String? = null,
        area: String? = null,
        type: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<MaterialJs>>>

    fun get(id: String): Promise<ApiResult<MaterialJs>>

    fun create(request: UpsertMaterialJs): Promise<ApiResult<MaterialJs>>

    fun update(
        id: String,
        request: UpsertMaterialJs,
    ): Promise<ApiResult<MaterialJs>>

    fun delete(id: String): Promise<ApiResult<EmptyJs>>
}
