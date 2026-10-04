package io.github.mfabisiak.hubmi.web.messaging

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.ReplyTemplateCatalog
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.mock.DemoAccount
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.MockThread
import io.github.mfabisiak.hubmi.web.mock.ROPS_TEAM_NAME
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.conflict
import io.github.mfabisiak.hubmi.web.mock.forbidden
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
                .map { it.thread.seenByCaller() }
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
            val author = backend.account
            val sentAt = nowIso()
            val thread =
                MockThread(
                    thread = ThreadDto(newId("watek"), subject.trim(), relatedIdeaId, isoDaysFromNow(0), unread = true),
                    messages =
                        listOfNotNull(
                            MessageDto(
                                newId("wiadomosc"),
                                author.displayName,
                                author.participantRole,
                                message.trim(),
                                sentAt,
                            ),
                            MessageDto(newId("wiadomosc"), ROPS_TEAM_NAME, ParticipantRole.ADMIN, AUTO_REPLY, sentAt)
                                .takeIf { author.participantRole == ParticipantRole.AUTHOR },
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
            val author = backend.account
            val message =
                MessageDto(newId("wiadomosc"), author.displayName, author.participantRole, text.trim(), nowIso())
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

    override fun assign(threadId: String): Promise<ApiResult<ThreadJs>> =
        backend.respond {
            val thread = ensureNotNull(findThread(threadId)) { notFound("wątek $threadId") }
            val caller = backend.account.displayName
            val current = thread.thread.assigneeName
            ensure(current == null || current == caller) { conflict("Ten wątek obsługuje już inny pracownik ROPS") }
            updateThread(thread) { it.copy(assigneeName = caller) }.toJs()
        }

    override fun unassign(threadId: String): Promise<ApiResult<ThreadJs>> =
        backend.respond {
            val thread = ensureNotNull(findThread(threadId)) { notFound("wątek $threadId") }
            val current = thread.thread.assigneeName
            ensure(current == null || current == backend.account.displayName || backend.account == DemoAccount.ADMIN) {
                forbidden("Wątek obsługuje inny pracownik ROPS")
            }
            updateThread(thread) { it.copy(assigneeName = null) }.toJs()
        }

    override fun replyTemplates(): Promise<ApiResult<Array<ReplyTemplateJs>>> =
        backend.respond { ReplyTemplateCatalog.all.map { it.toJs() }.toTypedArray() }

    private fun findThread(threadId: String): MockThread? = store.db.threads.firstOrNull { it.thread.id == threadId }

    private fun updateThread(
        thread: MockThread,
        change: (ThreadDto) -> ThreadDto,
    ): ThreadDto {
        val changed = change(thread.thread)
        store.update { db ->
            db.copy(threads = db.threads.map { if (it.thread.id == changed.id) it.copy(thread = changed) else it })
        }
        return changed.seenByCaller()
    }

    /** The stored thread says who handles it; whether that is the signed-in account depends on who looks. */
    private fun ThreadDto.seenByCaller(): ThreadDto =
        copy(assignedToMe = assigneeName != null && assigneeName == backend.account.displayName)
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
