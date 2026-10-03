package io.github.mfabisiak.hubmi.web.messaging

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.enumsOf
import io.github.mfabisiak.hubmi.web.fetch
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.send
import io.github.mfabisiak.hubmi.web.sendForUnit
import io.github.mfabisiak.hubmi.web.toPageJs
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

/** `/api/threads`: conversations of the caller; all calls need login. */
@JsExport
class ThreadsApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    fun list(
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ThreadJs>>> =
        scope.promiseResult {
            client
                .fetch<Threads, Page<ThreadDto>>(Threads(page = page, size = size))
                .map { it.toPageJs { thread -> thread.toJs() } }
        }

    fun create(
        subject: String,
        message: String,
        relatedIdeaId: String? = null,
    ): Promise<ApiResult<ThreadJs>> =
        scope.promiseResult {
            client
                .send<Threads, CreateThreadRequest, ThreadDto>(
                    HttpMethod.Post,
                    Threads(),
                    CreateThreadRequest(subject, message, relatedIdeaId),
                ).map { it.toJs() }
        }

    fun messages(threadId: String): Promise<ApiResult<Array<MessageJs>>> =
        scope.promiseResult {
            client
                .fetch<Threads.ById.Messages, List<MessageDto>>(
                    Threads.ById.Messages(parent = Threads.ById(id = threadId)),
                ).map { messages -> messages.map { it.toJs() }.toTypedArray() }
        }

    fun postMessage(
        threadId: String,
        text: String,
    ): Promise<ApiResult<MessageJs>> =
        scope.promiseResult {
            client
                .send<Threads.ById.Messages, PostMessageRequest, MessageDto>(
                    HttpMethod.Post,
                    Threads.ById.Messages(parent = Threads.ById(id = threadId)),
                    PostMessageRequest(text),
                ).map { it.toJs() }
        }
}

/** `/api/notifications`: notifications of the caller; all calls need login. */
@JsExport
class NotificationsApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    fun list(
        unreadOnly: Boolean = false,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<NotificationJs>>> =
        scope.promiseResult {
            client
                .fetch<Notifications, Page<NotificationDto>>(
                    Notifications(unreadOnly = unreadOnly, page = page, size = size),
                ).map { it.toPageJs { notification -> notification.toJs() } }
        }

    fun markRead(id: String): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult {
            client.sendForUnit(HttpMethod.Post, Notifications.Read(id = id)).map { EmptyJs() }
        }
}
