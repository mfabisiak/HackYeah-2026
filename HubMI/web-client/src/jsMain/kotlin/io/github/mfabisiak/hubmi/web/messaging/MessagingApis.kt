package io.github.mfabisiak.hubmi.web.messaging

import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import kotlin.js.Promise

/** `/api/threads`: conversations of the caller; all calls need login. */
@JsExport
interface ThreadsApi {
    fun list(
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<ThreadJs>>>

    fun create(
        subject: String,
        message: String,
        relatedIdeaId: String? = null,
    ): Promise<ApiResult<ThreadJs>>

    fun messages(threadId: String): Promise<ApiResult<Array<MessageJs>>>

    fun postMessage(
        threadId: String,
        text: String,
    ): Promise<ApiResult<MessageJs>>
}

/** `/api/notifications`: notifications of the caller; all calls need login. */
@JsExport
interface NotificationsApi {
    fun list(
        unreadOnly: Boolean = false,
        page: Int = PageRequest.DEFAULT_PAGE,
        size: Int = PageRequest.DEFAULT_SIZE,
    ): Promise<ApiResult<PageJs<NotificationJs>>>

    fun markRead(id: String): Promise<ApiResult<EmptyJs>>
}
