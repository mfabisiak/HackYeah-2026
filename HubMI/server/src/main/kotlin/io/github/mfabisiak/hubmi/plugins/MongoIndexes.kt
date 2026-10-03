package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.calls.ApplicationItem
import io.github.mfabisiak.hubmi.calls.GrantCallItem
import io.github.mfabisiak.hubmi.calls.applications
import io.github.mfabisiak.hubmi.calls.grantCalls
import io.github.mfabisiak.hubmi.challenges.ChallengeItem
import io.github.mfabisiak.hubmi.challenges.challenges
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import io.github.mfabisiak.hubmi.ideas.IdeaItem
import io.github.mfabisiak.hubmi.ideas.ideas
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.innovations
import io.github.mfabisiak.hubmi.matching.NeedItem
import io.github.mfabisiak.hubmi.matching.needs
import io.github.mfabisiak.hubmi.materials.MaterialItem
import io.github.mfabisiak.hubmi.materials.materials
import io.github.mfabisiak.hubmi.messaging.MessageItem
import io.github.mfabisiak.hubmi.messaging.NotificationItem
import io.github.mfabisiak.hubmi.messaging.ThreadItem
import io.github.mfabisiak.hubmi.messaging.messages
import io.github.mfabisiak.hubmi.messaging.notifications
import io.github.mfabisiak.hubmi.messaging.threads
import io.github.mfabisiak.hubmi.samples.SampleItem
import io.github.mfabisiak.hubmi.samples.samples
import io.github.mfabisiak.hubmi.tester.FeedbackItem
import io.github.mfabisiak.hubmi.tester.TestRequestItem
import io.github.mfabisiak.hubmi.tester.feedbacks
import io.github.mfabisiak.hubmi.tester.testRequests
import com.mongodb.client.model.Indexes as MongoIndexesHelper

object MongoIndexes {
    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            database.samples.createIndex(Indexes.ascending(SampleItem::slug), IndexOptions().unique(true))

            database.innovations.createIndex(Indexes.ascending(InnovationItem::areas))
            database.innovations.createIndex(Indexes.ascending(InnovationItem::targetGroups))

            database.challenges.createIndex(Indexes.ascending(ChallengeItem::area))

            database.materials.createIndex(Indexes.ascending(MaterialItem::type))
            database.materials.createIndex(Indexes.ascending(MaterialItem::areas))

            database.ideas.createIndex(Indexes.ascending(IdeaItem::authorId))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::applicantId))
            database.applications.createIndex(
                MongoIndexesHelper.compoundIndex(
                    Indexes.ascending(ApplicationItem::callId),
                    Indexes.ascending(ApplicationItem::applicantId),
                    Indexes.ascending(ApplicationItem::status),
                ),
            )
            database.needs.createIndex(Indexes.ascending(NeedItem::matchedInnovationIds))
            database.needs.createIndex(
                MongoIndexesHelper.compoundIndex(
                    Indexes.ascending(NeedItem::createdAt),
                    Indexes.ascending(NeedItem::areas),
                ),
            )
            database.feedbacks.createIndex(
                Indexes.ascending(FeedbackItem::innovationId, FeedbackItem::userId),
                IndexOptions().unique(true),
            )

            database.testRequests.createIndex(
                Indexes.ascending(TestRequestItem::innovationId, TestRequestItem::userId),
                IndexOptions().unique(true),
            )

            database.threads.createIndex(Indexes.descending(ThreadItem::lastMessageAt))
            database.threads.createIndex(Indexes.ascending(ThreadItem::participantIds))
            database.messages.createIndex(
                Indexes.ascending(
                    MessageItem::threadId,
                    MessageItem::sentAt,
                ),
            )
            database.notifications.createIndex(Indexes.descending(NotificationItem::createdAt))
            database.notifications.createIndex(
                Indexes.ascending(
                    NotificationItem::recipientId,
                    NotificationItem::read,
                ),
            )
            database.notifications.createIndex(Indexes.ascending(NotificationItem::targetRole))
        }.map { }
}
