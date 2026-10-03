package io.github.mfabisiak.hubmi.web.knowledge

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.Challenges
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.Materials
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.fetch
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.send
import io.github.mfabisiak.hubmi.web.sendForUnit
import io.github.mfabisiak.hubmi.web.toPageJs
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

/** `/api/challenges`: reading is public, create/update/delete need `admin`. */
@JsExport
class ChallengesApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    fun list(
        area: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ChallengeJs>>> =
        scope.promiseResult {
            either {
                val resource =
                    Challenges(
                        area = enumOrNull<SocialArea>(area, "area"),
                        page = page,
                        size = size,
                    )
                client.fetch<Challenges, Page<ChallengeDto>>(resource).bind().toPageJs { it.toJs() }
            }
        }

    fun get(id: String): Promise<ApiResult<ChallengeJs>> =
        scope.promiseResult {
            client.fetch<Challenges.ById, ChallengeDto>(Challenges.ById(id = id)).map { it.toJs() }
        }

    fun create(request: UpsertChallengeJs): Promise<ApiResult<ChallengeJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Challenges, UpsertChallengeRequest, ChallengeDto>(
                        HttpMethod.Post,
                        Challenges(),
                        request.toDto().bind(),
                    ).bind()
                    .toJs()
            }
        }

    fun update(
        id: String,
        request: UpsertChallengeJs,
    ): Promise<ApiResult<ChallengeJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Challenges.ById, UpsertChallengeRequest, ChallengeDto>(
                        HttpMethod.Put,
                        Challenges.ById(id = id),
                        request.toDto().bind(),
                    ).bind()
                    .toJs()
            }
        }

    fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult { client.sendForUnit(HttpMethod.Delete, Challenges.ById(id = id)).map { EmptyJs() } }
}

/** `/api/materials`: reading is public, create/update/delete need `admin`. */
@JsExport
class MaterialsApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    fun list(
        q: String? = null,
        area: String? = null,
        type: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<MaterialJs>>> =
        scope.promiseResult {
            either {
                val resource =
                    Materials(
                        q = q,
                        area = enumOrNull<SocialArea>(area, "area"),
                        type = enumOrNull<MaterialType>(type, "type"),
                        page = page,
                        size = size,
                    )
                client.fetch<Materials, Page<MaterialDto>>(resource).bind().toPageJs { it.toJs() }
            }
        }

    fun get(id: String): Promise<ApiResult<MaterialJs>> =
        scope.promiseResult {
            client.fetch<Materials.ById, MaterialDto>(Materials.ById(id = id)).map { it.toJs() }
        }

    fun create(request: UpsertMaterialJs): Promise<ApiResult<MaterialJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Materials, UpsertMaterialRequest, MaterialDto>(
                        HttpMethod.Post,
                        Materials(),
                        request.toDto().bind(),
                    ).bind()
                    .toJs()
            }
        }

    fun update(
        id: String,
        request: UpsertMaterialJs,
    ): Promise<ApiResult<MaterialJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Materials.ById, UpsertMaterialRequest, MaterialDto>(
                        HttpMethod.Put,
                        Materials.ById(id = id),
                        request.toDto().bind(),
                    ).bind()
                    .toJs()
            }
        }

    fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult { client.sendForUnit(HttpMethod.Delete, Materials.ById(id = id)).map { EmptyJs() } }
}
