package io.github.mfabisiak.hubmi.web.ideas

import arrow.core.flatMap
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.Applications
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.fetch
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.send
import io.github.mfabisiak.hubmi.web.sendWithoutBody
import io.github.mfabisiak.hubmi.web.toPageJs
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlin.js.Promise

@JsExport
class ApplicationsApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Applications submitted or drafted by the caller. */
    fun mine(
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ApplicationJs>>> =
        scope.promiseResult {
            client
                .fetch<Applications.Mine, Page<ApplicationDto>>(Applications.Mine(page = page, size = size))
                .map { it.toPageJs { app -> app.toJs() } }
        }

    fun get(id: String): Promise<ApiResult<ApplicationJs>> =
        scope.promiseResult {
            client.fetch<Applications.ById, ApplicationDto>(Applications.ById(id = id)).map { it.toJs() }
        }

    /** Creates a draft application for the given call (optionally prefilled from idea). */
    fun createDraft(
        callId: String,
        ideaId: String? = null,
    ): Promise<ApiResult<ApplicationJs>> =
        scope.promiseResult {
            client
                .send<Calls.ById.Applications, CreateApplicationDraftRequest, ApplicationDto>(
                    HttpMethod.Post,
                    Calls.ById.Applications(parent = Calls.ById(id = callId)),
                    CreateApplicationDraftRequest(ideaId),
                ).map { it.toJs() }
        }

    /** Saves a draft application by passing a raw JSON string of [SaveApplicationDraftRequest]. */
    fun saveDraft(
        id: String,
        bodyJson: String,
    ): Promise<ApiResult<ApplicationJs>> =
        scope.promiseResult {
            arrow.core.Either
                .catch { json.decodeFromString<SaveApplicationDraftRequest>(bodyJson) }
                .mapLeft {
                    io.github.mfabisiak.hubmi.web
                        .ApiErrorJs(400, "INVALID_BODY", it.message ?: "Invalid JSON")
                }.flatMap { request ->
                    client.send<Applications.ById, SaveApplicationDraftRequest, ApplicationDto>(
                        HttpMethod.Put,
                        Applications.ById(id = id),
                        request,
                    )
                }.map { it.toJs() }
        }

    /** Submits a draft application. */
    fun submit(id: String): Promise<ApiResult<ApplicationJs>> =
        scope.promiseResult {
            client
                .sendWithoutBody<Applications.ById.Submit, ApplicationDto>(
                    HttpMethod.Post,
                    Applications.ById.Submit(parent = Applications.ById(id = id)),
                ).map { it.toJs() }
        }
}
