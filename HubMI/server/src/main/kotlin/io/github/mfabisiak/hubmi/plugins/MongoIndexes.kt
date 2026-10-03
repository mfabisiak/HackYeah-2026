package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.challenges.ChallengeItem
import io.github.mfabisiak.hubmi.challenges.challenges
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.innovations
import io.github.mfabisiak.hubmi.matching.NeedItem
import io.github.mfabisiak.hubmi.matching.needs
import io.github.mfabisiak.hubmi.materials.MaterialItem
import io.github.mfabisiak.hubmi.materials.materials
import io.github.mfabisiak.hubmi.samples.SampleItem
import io.github.mfabisiak.hubmi.samples.samples
import io.github.mfabisiak.hubmi.tester.FeedbackItem
import io.github.mfabisiak.hubmi.tester.TestRequestItem
import io.github.mfabisiak.hubmi.tester.feedbacks
import io.github.mfabisiak.hubmi.tester.testRequests

object MongoIndexes {
    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            database.samples.createIndex(Indexes.ascending(SampleItem::slug), IndexOptions().unique(true))

            database.innovations.createIndex(Indexes.ascending(InnovationItem::areas))
            database.innovations.createIndex(Indexes.ascending(InnovationItem::targetGroups))

            database.challenges.createIndex(Indexes.ascending(ChallengeItem::area))

            database.materials.createIndex(Indexes.ascending(MaterialItem::type))
            database.materials.createIndex(Indexes.ascending(MaterialItem::areas))

            database.needs.createIndex(Indexes.ascending(NeedItem::matchedInnovationIds))
            database.feedbacks.createIndex(
                Indexes.ascending(FeedbackItem::innovationId, FeedbackItem::userId),
                IndexOptions().unique(true),
            )

            database.testRequests.createIndex(
                Indexes.ascending(TestRequestItem::innovationId, TestRequestItem::userId),
                IndexOptions().unique(true),
            )
        }.map { }
}
