package io.github.mfabisiak.hubmi.messaging

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
        try {
            when (event) {
                is IdeaSubmitted -> {
                    notificationRepository.create(
                        NotificationItem(
                            targetRole = Role.ADMIN,
                            type = NotificationType.IDEA_SUBMITTED,
                            title = "Nowy pomysł zgłoszony",
                            body = "Zgłoszono nowy pomysł: ${event.title}",
                            eventId = "IdeaSubmitted-${event.ideaId}",
                            createdAt = event.occurredAt,
                        ),
                    )
                }

                is IdeaStatusChanged -> {
                    val statusText =
                        when (event.newStatus) {
                            IdeaStatus.DRAFT -> "wersja robocza"
                            IdeaStatus.IN_REVIEW -> "w trakcie oceny"
                            IdeaStatus.ACCEPTED -> "zaakceptowany"
                            IdeaStatus.REJECTED -> "odrzucony"
                            IdeaStatus.SUBMITTED -> "zgłoszony"
                        }
                    notificationRepository.create(
                        NotificationItem(
                            recipientId = event.authorId,
                            type = NotificationType.IDEA_STATUS_CHANGED,
                            title = "Zmiana statusu pomysłu",
                            body = "Status Twojego pomysłu został zmieniony na: $statusText",
                            eventId = "IdeaStatusChanged-${event.ideaId}-${event.newStatus}",
                            createdAt = event.occurredAt,
                        ),
                    )
                }

                is MessageReceived -> {
                    if (event.recipientIds.isEmpty()) {
                        notificationRepository.create(
                            NotificationItem(
                                targetRole = Role.ADMIN,
                                type = NotificationType.MESSAGE_RECEIVED,
                                title = "Nowa wiadomość w wątku: ${event.threadSubject.take(50)}",
                                body = "${event.senderName}: ${event.text.take(100)}",
                                eventId = "MessageReceived-${event.messageId}-admin",
                                createdAt = event.occurredAt,
                            ),
                        )
                    } else {
                        for (recipientId in event.recipientIds) {
                            notificationRepository.create(
                                NotificationItem(
                                    recipientId = recipientId,
                                    type = NotificationType.MESSAGE_RECEIVED,
                                    title = "Nowa wiadomość w wątku: ${event.threadSubject.take(50)}",
                                    body = "${event.senderName}: ${event.text.take(100)}",
                                    eventId = "MessageReceived-${event.messageId}-$recipientId",
                                    createdAt = event.occurredAt,
                                ),
                            )
                        }
                    }
                }

                is CallPublished -> {
                    notificationRepository.create(
                        NotificationItem(
                            targetRole = Role.USER,
                            type = NotificationType.CALL_PUBLISHED,
                            title = "Nowy nabór wniosków",
                            body = "Ogłoszono nowy nabór: ${event.callTitle}",
                            eventId = "CallPublished-${event.callId}",
                            createdAt = event.occurredAt,
                        ),
                    )
                }

                is CallChanged -> {
                    notificationRepository.create(
                        NotificationItem(
                            targetRole = Role.USER,
                            type = NotificationType.CALL_CHANGED,
                            title = "Aktualizacja naboru wniosków",
                            body = "Zaktualizowano nabór: ${event.callTitle}",
                            eventId = "CallChanged-${event.callId}",
                            createdAt = event.occurredAt,
                        ),
                    )
                }
            }
        } catch (e: Throwable) {
            logger.error("Failed to process event $event in notification listener", e)
        }
    }
}
