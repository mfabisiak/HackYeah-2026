package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.mongoCatch
import io.github.mfabisiak.hubmi.repository.samples

object MongoIndexes {
    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            database.samples.createIndex(Indexes.ascending(SampleItem::slug), IndexOptions().unique(true))
        }.map { }
}
