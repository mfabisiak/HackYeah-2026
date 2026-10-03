package io.github.mfabisiak.hubmi.ideas

import io.github.mfabisiak.hubmi.api.IdeaStatus
import java.time.Instant

sealed interface DomainEvent {
    val occurredAt: Instant
}

data class IdeaSubmitted(
    val ideaId: String,
    val authorId: String,
    val title: String,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

data class IdeaStatusChanged(
    val ideaId: String,
    val authorId: String,
    val oldStatus: IdeaStatus,
    val newStatus: IdeaStatus,
    val adminComment: String?,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

interface EventPublisher {
    suspend fun publish(event: DomainEvent)
}

class NoOpEventPublisher : EventPublisher {
    override suspend fun publish(event: DomainEvent) {
        // No-op for now; to be consumed in BE-07
    }
}
