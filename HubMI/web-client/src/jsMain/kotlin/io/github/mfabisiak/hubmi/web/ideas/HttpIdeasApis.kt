package io.github.mfabisiak.hubmi.web.ideas

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.DeclarationsResponse
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
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

internal class HttpIdeasApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : IdeasApi {
    override fun create(request: CreateIdeaJs): Promise<ApiResult<IdeaJs>> =
        scope.promiseResult {
            either {
                client
                    .send<Ideas, CreateIdeaRequest, IdeaDto>(HttpMethod.Post, Ideas(), request.toDto().bind())
                    .bind()
                    .toJs()
            }
        }

    override fun mine(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<IdeaJs>>> =
        scope.promiseResult {
            client
                .fetch<Ideas.Mine, Page<IdeaDto>>(Ideas.Mine(page = page, size = size))
                .map { it.toPageJs { idea -> idea.toJs() } }
        }

    override fun get(id: String): Promise<ApiResult<IdeaJs>> =
        scope.promiseResult { client.fetch<Ideas.ById, IdeaDto>(Ideas.ById(id = id)).map { it.toJs() } }
}

internal class HttpCallsApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : CallsApi {
    override fun list(status: String?): Promise<ApiResult<Array<GrantCallJs>>> =
        scope.promiseResult {
            either {
                client
                    .fetch<Calls, List<GrantCallDto>>(Calls(status = enumOrNull<CallStatus>(status, "status")))
                    .bind()
                    .map { it.toJs() }
                    .toTypedArray()
            }
        }

    override fun active(): Promise<ApiResult<Array<GrantCallJs>>> =
        scope.promiseResult {
            client
                .fetch<Calls.Active, List<GrantCallDto>>(Calls.Active())
                .map { calls -> calls.map { it.toJs() }.toTypedArray() }
        }

    override fun get(id: String): Promise<ApiResult<GrantCallJs>> =
        scope.promiseResult { client.fetch<Calls.ById, GrantCallDto>(Calls.ById(id = id)).map { it.toJs() } }

    override fun declarations(
        id: String,
        applicantType: String?,
    ): Promise<ApiResult<DeclarationsJs>> =
        scope.promiseResult {
            either {
                val resource =
                    Calls.ById.Declarations(
                        parent = Calls.ById(id = id),
                        applicantType = enumOrNull<ApplicantType>(applicantType, "applicantType"),
                    )
                client.fetch<Calls.ById.Declarations, DeclarationsResponse>(resource).bind().toJs()
            }
        }

    override fun create(request: UpsertCallJs): Promise<ApiResult<GrantCallJs>> =
        scope.promiseResult {
            client
                .send<Calls, UpsertCallRequest, GrantCallDto>(HttpMethod.Post, Calls(), request.toDto())
                .map { it.toJs() }
        }

    override fun update(
        id: String,
        request: UpsertCallJs,
    ): Promise<ApiResult<GrantCallJs>> =
        scope.promiseResult {
            client
                .send<Calls.ById, UpsertCallRequest, GrantCallDto>(HttpMethod.Put, Calls.ById(id = id), request.toDto())
                .map { it.toJs() }
        }

    override fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult { client.sendForUnit(HttpMethod.Delete, Calls.ById(id = id)).map { EmptyJs() } }

    override fun apply(
        callId: String,
        ideaId: String?,
    ): Promise<ApiResult<ApplicationJs>> =
        scope.promiseResult {
            client
                .send<Calls.ById.Applications, CreateApplicationDraftRequest, ApplicationDto>(
                    HttpMethod.Post,
                    Calls.ById.Applications(parent = Calls.ById(id = callId)),
                    CreateApplicationDraftRequest(ideaId),
                ).map { it.toJs() }
        }
}
