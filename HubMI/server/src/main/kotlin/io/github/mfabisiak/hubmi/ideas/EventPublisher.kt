package io.github.mfabisiak.hubmi.ideas

import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.ParticipantRole
import java.time.Instant

sealed interface DomainEvent {
    val occurredAt: Instant
}

data class IdeaSubmitted(
    val ideaId: String,
    val authorId: String,
    val title: String,
    override val occurredAt: Instant,
) : DomainEvent

data class IdeaStatusChanged(
    val ideaId: String,
    val authorId: String,
    val oldStatus: IdeaStatus,
    val newStatus: IdeaStatus,
    override val occurredAt: Instant,
) : DomainEvent

data class MessageReceived(
    val threadId: String,
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: ParticipantRole,
    val recipientIds: Set<String>,
    val threadSubject: String,
    val text: String,
    override val occurredAt: Instant,
) : DomainEvent

data class CallPublished(
    val callId: String,
    val callTitle: String,
    override val occurredAt: Instant,
) : DomainEvent

data class CallChanged(
    val callId: String,
    val callTitle: String,
    override val occurredAt: Instant,
) : DomainEvent

interface EventPublisher {
    suspend fun publish(event: DomainEvent)
}
