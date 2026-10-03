package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.models.ApplicationItem
import io.github.mfabisiak.hubmi.models.GrantCallItem
import io.github.mfabisiak.hubmi.models.IdeaItem
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.applications
import io.github.mfabisiak.hubmi.repository.grantCalls
import io.github.mfabisiak.hubmi.repository.ideas
import io.github.mfabisiak.hubmi.repository.mongoCatch
import io.github.mfabisiak.hubmi.repository.samples

object MongoIndexes {
    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            database.samples.createIndex(Indexes.ascending(SampleItem::slug), IndexOptions().unique(true))
            database.ideas.createIndex(Indexes.ascending(IdeaItem::authorId))
            database.ideas.createIndex(Indexes.ascending(IdeaItem::status))
            database.grantCalls.createIndex(Indexes.descending(GrantCallItem::opensAt))
            database.grantCalls.createIndex(Indexes.descending(GrantCallItem::closesAt))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::callId))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::applicantId))
            database.applications.createIndex(Indexes.ascending(ApplicationItem::ideaId))
        }.map { }
}
