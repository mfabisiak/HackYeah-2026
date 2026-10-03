package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.models.ChallengeItem
import io.github.mfabisiak.hubmi.models.InnovationItem
import io.github.mfabisiak.hubmi.models.MaterialItem
import io.github.mfabisiak.hubmi.models.NeedItem
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.challenges
import io.github.mfabisiak.hubmi.repository.innovations
import io.github.mfabisiak.hubmi.repository.materials
import io.github.mfabisiak.hubmi.repository.mongoCatch
import io.github.mfabisiak.hubmi.repository.needs
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

            database.needs.createIndex(Indexes.ascending(NeedItem::matchedInnovationIds))
        }.map { }
}
