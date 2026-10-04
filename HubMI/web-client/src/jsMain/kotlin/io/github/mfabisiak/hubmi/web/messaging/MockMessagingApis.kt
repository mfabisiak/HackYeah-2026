package io.github.mfabisiak.hubmi.web.messaging

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.mock.DEMO_AUTHOR_NAME
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.MockThread
import io.github.mfabisiak.hubmi.web.mock.ROPS_TEAM_NAME
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.isoDaysFromNow
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlin.js.Promise

private const val AUTO_REPLY =
    "Dziękujemy za wiadomość. To jest wersja demonstracyjna, więc odpowiedź powstała automatycznie. " +
        "W prawdziwym Hubie odpowie Ci ktoś z zespołu ROPS."

internal class MockThreadsApi(
    private val backend: MockBackend,
) : ThreadsApi {
    private val store = backend.store

    override fun list(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<ThreadJs>>> =
        backend.respond {
            store.db.threads
                .map { it.thread }
                .sortedByDescending { it.lastMessageAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun create(
        subject: String,
        message: String,
        relatedIdeaId: String?,
    ): Promise<ApiResult<ThreadJs>> =
        backend.respond {
            val errors =
                listOfNotNull(
                    blank("subject").takeIf { subject.isBlank() },
                    blank("message").takeIf { message.isBlank() },
                )
            ensure(errors.isEmpty()) { invalid(*errors.toTypedArray()) }
            val sentAt = nowIso()
            val thread =
                MockThread(
                    thread = ThreadDto(newId("watek"), subject.trim(), relatedIdeaId, isoDaysFromNow(0), unread = true),
                    messages =
                        listOf(
                            MessageDto(
                                newId("wiadomosc"),
                                DEMO_AUTHOR_NAME,
                                ParticipantRole.AUTHOR,
                                message.trim(),
                                sentAt,
                            ),
                            MessageDto(newId("wiadomosc"), ROPS_TEAM_NAME, ParticipantRole.ADMIN, AUTO_REPLY, sentAt),
                        ),
                )
            store.update { it.copy(threads = it.threads + thread) }
            thread.thread.toJs()
        }

    override fun messages(threadId: String): Promise<ApiResult<Array<MessageJs>>> =
        backend.respond {
            val thread =
                ensureNotNull(store.db.threads.firstOrNull { it.thread.id == threadId }) { notFound("wątek $threadId") }
            store.update { db ->
                db.copy(
                    threads =
                        db.threads.map {
                            if (it.thread.id ==
                                threadId
                            ) {
                                it.copy(thread = it.thread.copy(unread = false))
                            } else {
                                it
                            }
                        },
                )
            }
            thread.messages
                .sortedBy { it.sentAt }
                .map { it.toJs() }
                .toTypedArray()
        }

    override fun postMessage(
        threadId: String,
        text: String,
    ): Promise<ApiResult<MessageJs>> =
        backend.respond {
            ensure(text.isNotBlank()) { invalid(blank("text")) }
            ensure(store.db.threads.any { it.thread.id == threadId }) { notFound("wątek $threadId") }
            val message =
                MessageDto(newId("wiadomosc"), DEMO_AUTHOR_NAME, ParticipantRole.AUTHOR, text.trim(), nowIso())
            store.update { db ->
                db.copy(
                    threads =
                        db.threads.map {
                            if (it.thread.id == threadId) {
                                MockThread(it.thread.copy(lastMessageAt = message.sentAt), it.messages + message)
                            } else {
                                it
                            }
                        },
                )
            }
            message.toJs()
        }
}

internal class MockNotificationsApi(
    private val backend: MockBackend,
) : NotificationsApi {
    private val store = backend.store

    override fun list(
        unreadOnly: Boolean,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<NotificationJs>>> =
        backend.respond {
            store.db.notifications
                .filter { !unreadOnly || !it.read }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun markRead(id: String): Promise<ApiResult<EmptyJs>> =
        backend.respond {
            ensure(store.db.notifications.any { it.id == id }) { notFound("powiadomienie $id") }
            store.update { db ->
                db.copy(
                    notifications =
                        db.notifications.map {
                            if (it.id ==
                                id
                            ) {
                                it.copy(read = true)
                            } else {
                                it
                            }
                        },
                )
            }
            EmptyJs()
        }
}
