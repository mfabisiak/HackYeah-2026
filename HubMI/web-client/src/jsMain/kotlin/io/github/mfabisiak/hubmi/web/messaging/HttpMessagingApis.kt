package io.github.mfabisiak.hubmi.web.messaging

import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.fetch
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.send
import io.github.mfabisiak.hubmi.web.sendForUnit
import io.github.mfabisiak.hubmi.web.toPageJs
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

internal class HttpThreadsApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : ThreadsApi {
    override fun list(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<ThreadJs>>> =
        scope.promiseResult {
            client
                .fetch<Threads, Page<ThreadDto>>(Threads(page = page, size = size))
                .map { it.toPageJs { thread -> thread.toJs() } }
        }

    override fun create(
        subject: String,
        message: String,
        relatedIdeaId: String?,
    ): Promise<ApiResult<ThreadJs>> =
        scope.promiseResult {
            client
                .send<Threads, CreateThreadRequest, ThreadDto>(
                    HttpMethod.Post,
                    Threads(),
                    CreateThreadRequest(subject, message, relatedIdeaId),
                ).map { it.toJs() }
        }

    override fun messages(threadId: String): Promise<ApiResult<Array<MessageJs>>> =
        scope.promiseResult {
            client
                .fetch<Threads.ById.Messages, List<MessageDto>>(
                    Threads.ById.Messages(parent = Threads.ById(id = threadId)),
                ).map { messages -> messages.map { it.toJs() }.toTypedArray() }
        }

    override fun postMessage(
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

internal class HttpNotificationsApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : NotificationsApi {
    override fun list(
        unreadOnly: Boolean,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<NotificationJs>>> =
        scope.promiseResult {
            client
                .fetch<Notifications, Page<NotificationDto>>(
                    Notifications(unreadOnly = unreadOnly, page = page, size = size),
                ).map { it.toPageJs { notification -> notification.toJs() } }
        }

    override fun markRead(id: String): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult {
            client.sendForUnit(HttpMethod.Post, Notifications.Read(id = id)).map { EmptyJs() }
        }
}
