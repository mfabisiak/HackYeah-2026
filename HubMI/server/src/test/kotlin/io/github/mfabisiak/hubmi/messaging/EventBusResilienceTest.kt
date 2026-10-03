package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.ideas.DomainEvent
import io.github.mfabisiak.hubmi.ideas.IdeaSubmitted
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.*

class EventBusResilienceTest {
    class FailingNotificationRepository :
        NotificationRepository(
            MongoClient.create(MongoTestEnvironment.connectionString).getDatabase("dummy"),
        ) {
        override suspend fun create(item: NotificationItem): Either<RepositoryError, NotificationItem> =
            throw RuntimeException("Simulated unexpected database failure")
    }

    @Test
    fun consumerFailureDoesNotThrowOrPropagateException() =
        runTest {
            val failingRepo = FailingNotificationRepository()
            val bus = CoroutineEventBus()
            val listener = NotificationEventListener(bus, failingRepo)

            val event: DomainEvent =
                IdeaSubmitted(
                    ideaId = "idea-123",
                    authorId = "author-123",
                    title = "Test",
                    occurredAt = Instant.now(),
                )

            assertDoesNotThrow {
                listener.handleEvent(event)
            }
        }

    private fun assertDoesNotThrow(block: suspend () -> Unit) {
        try {
            runBlocking { block() }
        } catch (e: Throwable) {
            fail("Expected not to throw, but threw $e")
        }
    }
}
