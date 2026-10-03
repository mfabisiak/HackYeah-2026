package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.ideas.CallChanged
import io.github.mfabisiak.hubmi.ideas.CallPublished
import io.github.mfabisiak.hubmi.ideas.DomainEvent
import io.github.mfabisiak.hubmi.ideas.IdeaStatusChanged
import io.github.mfabisiak.hubmi.ideas.IdeaSubmitted
import io.github.mfabisiak.hubmi.ideas.MessageReceived
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory

class NotificationEventListener(
    private val eventBus: EventBus,
    private val notificationRepository: NotificationRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val logger = LoggerFactory.getLogger(NotificationEventListener::class.java)

    fun start() {
        eventBus.events
            .onEach { event -> handleEvent(event) }
            .launchIn(scope)
    }

    suspend fun handleEvent(event: DomainEvent) {
        Either
            .catch {
                event.toNotifications().forEach { notification ->
                    notificationRepository
                        .create(notification)
                        .onLeft { logger.error("Failed to store notification for event {}: {}", event, it) }
                }
            }.onLeft { logger.error("Failed to process event $event in notification listener", it) }
    }
}

private fun DomainEvent.toNotifications(): List<NotificationItem> =
    when (this) {
        is IdeaSubmitted -> {
            listOf(
                NotificationItem(
                    targetRole = Role.ADMIN,
                    type = NotificationType.IDEA_SUBMITTED,
                    title = "Nowy pomysł zgłoszony",
                    body = "Zgłoszono nowy pomysł: $title",
                    eventId = "IdeaSubmitted-$ideaId",
                    createdAt = occurredAt,
                ),
            )
        }

        is IdeaStatusChanged -> {
            listOf(
                NotificationItem(
                    recipientId = authorId,
                    type = NotificationType.IDEA_STATUS_CHANGED,
                    title = "Zmiana statusu pomysłu",
                    body = "Status Twojego pomysłu został zmieniony na: ${newStatus.label}",
                    eventId = "IdeaStatusChanged-$ideaId-$newStatus",
                    createdAt = occurredAt,
                ),
            )
        }

        is MessageReceived -> {
            if (recipientIds.isEmpty()) {
                listOf(messageNotification(eventSuffix = "admin", targetRole = Role.ADMIN))
            } else {
                recipientIds.map { messageNotification(eventSuffix = it, recipientId = it) }
            }
        }

        is CallPublished -> {
            listOf(
                NotificationItem(
                    targetRole = Role.USER,
                    type = NotificationType.CALL_PUBLISHED,
                    title = "Nowy nabór wniosków",
                    body = "Ogłoszono nowy nabór: $callTitle",
                    eventId = "CallPublished-$callId",
                    createdAt = occurredAt,
                ),
            )
        }

        is CallChanged -> {
            listOf(
                NotificationItem(
                    targetRole = Role.USER,
                    type = NotificationType.CALL_CHANGED,
                    title = "Aktualizacja naboru wniosków",
                    body = "Zaktualizowano nabór: $callTitle",
                    eventId = "CallChanged-$callId",
                    createdAt = occurredAt,
                ),
            )
        }
    }

private fun MessageReceived.messageNotification(
    eventSuffix: String,
    recipientId: String? = null,
    targetRole: Role? = null,
): NotificationItem =
    NotificationItem(
        recipientId = recipientId,
        targetRole = targetRole,
        type = NotificationType.MESSAGE_RECEIVED,
        title = "Nowa wiadomość w wątku: ${threadSubject.take(50)}",
        body = "$senderName: ${text.take(100)}",
        eventId = "MessageReceived-$messageId-$eventSuffix",
        createdAt = occurredAt,
    )

private val IdeaStatus.label: String
    get() =
        when (this) {
            IdeaStatus.DRAFT -> "wersja robocza"
            IdeaStatus.IN_REVIEW -> "w trakcie oceny"
            IdeaStatus.ACCEPTED -> "zaakceptowany"
            IdeaStatus.REJECTED -> "odrzucony"
            IdeaStatus.SUBMITTED -> "zgłoszony"
        }
