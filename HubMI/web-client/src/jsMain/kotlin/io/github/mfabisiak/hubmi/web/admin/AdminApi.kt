package io.github.mfabisiak.hubmi.web.admin

import arrow.core.raise.either
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
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.TrendsDto
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.api.UpdateTestRequestStatusRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
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

/** `/api/admin`: every call needs the `admin` role. */
@JsExport
class AdminApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    /** Aggregated needs by area and municipality over the last [months] months. */
    fun trends(months: Int = 6): Promise<ApiResult<TrendsJs>> =
        scope.promiseResult {
            client.fetch<AdminTrends, TrendsDto>(AdminTrends(months = months)).map { it.toJs() }
        }

    /** Admin dashboard counters. */
    fun summary(): Promise<ApiResult<AdminSummaryJs>> =
        scope.promiseResult {
            client.fetch<AdminSummary, AdminSummaryDto>(AdminSummary()).map { it.toJs() }
        }

    /** Moderation queue. @param status `IdeaStatus` name to filter by. */
    fun ideas(
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
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

    /** Changes the status of an idea and leaves a comment for its author. */
    fun updateIdeaStatus(
        id: String,
        status: String,
        comment: String? = null,
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

    /** Grant applications list for admin. */
    fun applications(
        callId: String? = null,
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
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

    /** Ratings and comments of testers, newest first; optionally for one innovation. */
    fun feedback(
        innovationId: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<AdminFeedbackJs>>> =
        scope.promiseResult {
            client
                .fetch<AdminFeedback, Page<AdminFeedbackDto>>(
                    AdminFeedback(innovationId = innovationId, page = page, size = size),
                ).map { result -> result.toPageJs { it.toJs() } }
        }

    /** Declarations of willingness to test. @param status `TestRequestStatus` name to filter by. */
    fun testRequests(
        innovationId: String? = null,
        status: String? = null,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
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

    /** Accepts or declines a `NEW` test request (`ACCEPTED` or `DECLINED`); a decision is final. */
    fun decideTestRequest(
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
}
