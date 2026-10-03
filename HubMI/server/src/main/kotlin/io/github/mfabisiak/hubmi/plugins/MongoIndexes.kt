package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.mongoCatch
import org.bson.Document

object MongoIndexes {
    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            // Exemplary collection: unique index on slug
            database
                .getCollection<Document>("samples")
                .createIndex(Indexes.ascending("slug"), IndexOptions().unique(true))

            // Innovations collection: text search and filter indices
            val innovations = database.getCollection<Document>("innovations")
            innovations.createIndex(
                Document(
                    mapOf(
                        "title" to "text",
                        "summary" to "text",
                        "description" to "text",
                    ),
                ),
                IndexOptions().weights(Document(mapOf("title" to 10, "summary" to 5, "description" to 1))),
            )
            innovations.createIndex(Indexes.ascending("areas"))
            innovations.createIndex(Indexes.ascending("targetGroups"))

            // Challenges collection
            val challenges = database.getCollection<Document>("challenges")
            challenges.createIndex(
                Document(mapOf("title" to "text", "description" to "text")),
            )
            challenges.createIndex(Indexes.ascending("area"))

            // Materials collection
            val materials = database.getCollection<Document>("materials")
            materials.createIndex(
                Document(mapOf("title" to "text", "description" to "text")),
            )
            materials.createIndex(Indexes.ascending("area"))
        }
}
