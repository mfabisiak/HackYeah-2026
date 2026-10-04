package io.github.mfabisiak.hubmi.web.innovations

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.CreateFeedbackRequest
import io.github.mfabisiak.hubmi.api.CreateTestRequest
import io.github.mfabisiak.hubmi.api.FeedbackDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestDto
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
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

internal class HttpInnovationsApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : InnovationsApi {
    override fun list(
        q: String?,
        area: String?,
        targetGroup: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<InnovationSummaryJs>>> =
        scope.promiseResult {
            either {
                val resource =
                    Innovations(
                        q = q,
                        area = enumOrNull<SocialArea>(area, "area"),
                        targetGroup = enumOrNull<TargetGroup>(targetGroup, "targetGroup"),
                        page = page,
                        size = size,
                    )
                client
                    .fetch<Innovations, Page<InnovationSummary>>(resource)
                    .bind()
                    .toPageJs { it.toJs() }
            }
        }

    override fun get(id: String): Promise<ApiResult<InnovationJs>> =
        scope.promiseResult {
            client.fetch<Innovations.ById, InnovationDto>(Innovations.ById(id = id)).map { it.toJs() }
        }

    override fun create(request: UpsertInnovationJs): Promise<ApiResult<InnovationJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Innovations, UpsertInnovationRequest, InnovationDto>(
                        HttpMethod.Post,
                        Innovations(),
                        request.toDto().bind(),
                    ).bind()
                    .toJs()
            }
        }

    override fun update(
        id: String,
        request: UpsertInnovationJs,
    ): Promise<ApiResult<InnovationJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Innovations.ById, UpsertInnovationRequest, InnovationDto>(
                        HttpMethod.Put,
                        Innovations.ById(id = id),
                        request.toDto().bind(),
                    ).bind()
                    .toJs()
            }
        }

    override fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult { client.sendForUnit(HttpMethod.Delete, Innovations.ById(id = id)).map { EmptyJs() } }

    override fun requestTest(
        id: String,
        note: String?,
    ): Promise<ApiResult<TestRequestJs>> =
        scope.promiseResult {
            client
                .send<Innovations.ById.TestRequest, CreateTestRequest, TestRequestDto>(
                    HttpMethod.Put,
                    Innovations.ById.TestRequest(parent = Innovations.ById(id = id)),
                    CreateTestRequest(note),
                ).map { it.toJs() }
        }

    override fun myTestRequest(id: String): Promise<ApiResult<TestRequestJs>> =
        scope.promiseResult {
            client
                .fetch<Innovations.ById.TestRequest, TestRequestDto>(
                    Innovations.ById.TestRequest(parent = Innovations.ById(id = id)),
                ).map { it.toJs() }
        }

    override fun myFeedback(id: String): Promise<ApiResult<FeedbackJs>> =
        scope.promiseResult {
            client
                .fetch<Innovations.ById.Feedback, FeedbackDto>(
                    Innovations.ById.Feedback(parent = Innovations.ById(id = id)),
                ).map { it.toJs() }
        }

    override fun sendFeedback(
        id: String,
        rating: Int,
        comment: String?,
        suggestion: String?,
    ): Promise<ApiResult<FeedbackJs>> =
        scope.promiseResult {
            client
                .send<Innovations.ById.Feedback, CreateFeedbackRequest, FeedbackDto>(
                    HttpMethod.Put,
                    Innovations.ById.Feedback(parent = Innovations.ById(id = id)),
                    CreateFeedbackRequest(rating, comment, suggestion),
                ).map { it.toJs() }
        }
}
