package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.models.ApplicationItem
import io.github.mfabisiak.hubmi.models.ChallengeItem
import io.github.mfabisiak.hubmi.models.GrantCallItem
import io.github.mfabisiak.hubmi.models.IdeaItem
import io.github.mfabisiak.hubmi.models.InnovationItem
import io.github.mfabisiak.hubmi.models.MaterialItem
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.applications
import io.github.mfabisiak.hubmi.repository.challenges
import io.github.mfabisiak.hubmi.repository.grantCalls
import io.github.mfabisiak.hubmi.repository.ideas
import io.github.mfabisiak.hubmi.repository.innovations
import io.github.mfabisiak.hubmi.repository.materials
import io.github.mfabisiak.hubmi.repository.mongoCatch
import io.github.mfabisiak.hubmi.repository.samples

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
            database.ideas.createIndex(Indexes.ascending(IdeaItem::status))
            database.grantCalls.createIndex(Indexes.descending(GrantCallItem::opensAt))
            database.grantCalls.createIndex(Indexes.descending(GrantCallItem::closesAt))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::callId))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::applicantId))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::ideaId))
        }.map { }
}
