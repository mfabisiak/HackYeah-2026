package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.mongoCatch
import org.bson.Document
import org.slf4j.LoggerFactory

class DatabaseSeeder(
    private val database: MongoDatabase,
    private val config: AppConfig,
) {
    private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)

    suspend fun seedIfNeeded(): Either<RepositoryError, Unit> {
        if (!config.seed) {
            logger.info("Database seeding is disabled (SEED=false)")
            return Either.Right(Unit)
        }

        logger.info("Running database seeders (SEED=true)...")
        return mongoCatch {
            val samples = database.getCollection<Document>("samples")
            val sampleItems =
                listOf(
                    Document(
                        mapOf(
                            "slug" to "wzorcowa-innowacja",
                            "name" to "Wzorcowa Innowacja Społeczna",
                            "description" to "Przykładowa innowacja seedowa",
                        ),
                    ),
                    Document(
                        mapOf(
                            "slug" to "klub-seniora",
                            "name" to "Cyfrowy Klub Seniora",
                            "description" to "Wsparcie cyfrowe dla seniorów w Małopolsce",
                        ),
                    ),
                )

            for (item in sampleItems) {
                samples.replaceOne(
                    Filters.eq("slug", item.getString("slug")),
                    item,
                    ReplaceOptions().upsert(true),
                )
            }
            logger.info("Database seeding completed.")
        }
    }
}
