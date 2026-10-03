package io.github.mfabisiak.hubmi.messaging

import io.github.mfabisiak.hubmi.ideas.DomainEvent
import io.github.mfabisiak.hubmi.ideas.EventPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory

interface EventBus : EventPublisher {
    val events: SharedFlow<DomainEvent>
}

class CoroutineEventBus(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : EventBus {
    private val logger = LoggerFactory.getLogger(CoroutineEventBus::class.java)
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    override suspend fun publish(event: DomainEvent) {
        logger.debug("Publishing domain event: {}", event)
        _events.emit(event)
    }
}
