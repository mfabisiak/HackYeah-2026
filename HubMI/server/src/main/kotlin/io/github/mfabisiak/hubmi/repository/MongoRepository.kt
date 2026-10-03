package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.config.AppConfig
import org.bson.Document

/** Owns the Mongo client; repositories receive only [database]. */
class MongoRepository(
    config: AppConfig,
) : AutoCloseable {
    val client: MongoClient = MongoClient.create(config.mongoUri)
    val database: MongoDatabase = client.getDatabase(config.mongoDatabase)

    suspend fun ping(): Either<RepositoryError, Document> =
        Either
            .catch {
                database.runCommand<Document>(Document("ping", 1))
            }.mapLeft { RepositoryError.DatabaseException(it) }

    override fun close(): Unit = client.close()
}
