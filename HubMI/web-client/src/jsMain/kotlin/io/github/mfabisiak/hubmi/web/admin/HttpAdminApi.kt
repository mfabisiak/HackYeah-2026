package io.github.mfabisiak.hubmi.web.admin

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.AdminAdaptations
import io.github.mfabisiak.hubmi.api.AdminApplications
import io.github.mfabisiak.hubmi.api.AdminFeedback
import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminIdeas
import io.github.mfabisiak.hubmi.api.AdminSummary
import io.github.mfabisiak.hubmi.api.AdminSummaryDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.AdminTestRequests
import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.TrendsDto
import io.github.mfabisiak.hubmi.api.UpdateAdaptationStatusRequest
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.api.UpdateTestRequestStatusRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.adaptations.AdaptationJs
import io.github.mfabisiak.hubmi.web.adaptations.toJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.fetch
import io.github.mfabisiak.hubmi.web.ideas.ApplicationJs
import io.github.mfabisiak.hubmi.web.ideas.IdeaJs
import io.github.mfabisiak.hubmi.web.ideas.toJs
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.send
import io.github.mfabisiak.hubmi.web.toPageJs
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

internal class HttpAdminApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : AdminApi {
    override fun trends(months: Int): Promise<ApiResult<TrendsJs>> =
        scope.promiseResult {
            client.fetch<AdminTrends, TrendsDto>(AdminTrends(months = months)).map { it.toJs() }
        }

    override fun summary(): Promise<ApiResult<AdminSummaryJs>> =
        scope.promiseResult {
            client.fetch<AdminSummary, AdminSummaryDto>(AdminSummary()).map { it.toJs() }
        }

    override fun ideas(
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<IdeaJs>>> =
        scope.promiseResult {
            either {
                client
                    .fetch<AdminIdeas, Page<IdeaDto>>(
                        AdminIdeas(status = enumOrNull<IdeaStatus>(status, "status"), page = page, size = size),
                    ).bind()
                    .toPageJs { it.toJs() }
            }
        }

    override fun updateIdeaStatus(
        id: String,
        status: String,
        comment: String?,
    ): Promise<ApiResult<IdeaJs>> =
        scope.promiseResult {
            either {
                client
                    .send<AdminIdeas.Status, UpdateIdeaStatusRequest, IdeaDto>(
                        HttpMethod.Patch,
                        AdminIdeas.Status(id = id),
                        UpdateIdeaStatusRequest(enumOf<IdeaStatus>(status, "status"), comment),
                    ).bind()
                    .toJs()
            }
        }

    override fun applications(
        callId: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<ApplicationJs>>> =
        scope.promiseResult {
            either {
                client
                    .fetch<AdminApplications, Page<ApplicationDto>>(
                        AdminApplications(
                            callId = callId,
                            status = enumOrNull<ApplicationStatus>(status, "status"),
                            page = page,
                            size = size,
                        ),
                    ).bind()
                    .toPageJs { it.toJs() }
            }
        }

    override fun feedback(
        innovationId: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdminFeedbackJs>>> =
        scope.promiseResult {
            client
                .fetch<AdminFeedback, Page<AdminFeedbackDto>>(
                    AdminFeedback(innovationId = innovationId, page = page, size = size),
                ).map { result -> result.toPageJs { it.toJs() } }
        }

    override fun testRequests(
        innovationId: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdminTestRequestJs>>> =
        scope.promiseResult {
            either {
                client
                    .fetch<AdminTestRequests, Page<AdminTestRequestDto>>(
                        AdminTestRequests(
                            innovationId = innovationId,
                            status = enumOrNull<TestRequestStatus>(status, "status"),
                            page = page,
                            size = size,
                        ),
                    ).bind()
                    .toPageJs { it.toJs() }
            }
        }

    override fun decideTestRequest(
        id: String,
        status: String,
    ): Promise<ApiResult<AdminTestRequestJs>> =
        scope.promiseResult {
            either {
                client
                    .send<AdminTestRequests.Status, UpdateTestRequestStatusRequest, AdminTestRequestDto>(
                        HttpMethod.Patch,
                        AdminTestRequests.Status(id = id),
                        UpdateTestRequestStatusRequest(enumOf<TestRequestStatus>(status, "status")),
                    ).bind()
                    .toJs()
            }
        }

    override fun adaptations(
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdaptationJs>>> =
        scope.promiseResult {
            either {
                client
                    .fetch<AdminAdaptations, Page<AdaptationDto>>(
                        AdminAdaptations(
                            status = enumOrNull<AdaptationStatus>(status, "status"),
                            page = page,
                            size = size,
                        ),
                    ).bind()
                    .toPageJs { it.toJs() }
            }
        }

    override fun reviewAdaptation(
        id: String,
        status: String,
        comment: String?,
    ): Promise<ApiResult<AdaptationJs>> =
        scope.promiseResult {
            either {
                client
                    .send<AdminAdaptations.Status, UpdateAdaptationStatusRequest, AdaptationDto>(
                        HttpMethod.Patch,
                        AdminAdaptations.Status(id = id),
                        UpdateAdaptationStatusRequest(enumOf<AdaptationStatus>(status, "status"), comment),
                    ).bind()
                    .toJs()
            }
        }
}
