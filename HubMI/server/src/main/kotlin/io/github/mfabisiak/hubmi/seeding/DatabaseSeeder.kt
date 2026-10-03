package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.catching
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.Document
import org.slf4j.LoggerFactory

@Serializable
data class SeedSampleItem(
    val seedKey: String,
    val slug: String,
    val name: String,
    val description: String,
)

class DatabaseSeeder(
    private val database: MongoDatabase,
    private val config: AppConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)

    suspend fun seedIfNeeded(): Either<RepositoryError, Unit> {
        if (!config.seed) {
            logger.info("Database seeding is disabled (SEED=false)")
            return Either.Right(Unit)
        }

        logger.info("Running database seeders from resources/seed/ (SEED=true)...")
        return catching {
            val samplesCollection = database.getCollection<Document>("samples")
            val seedItems = loadSeedSamples()

            for (item in seedItems) {
                val doc =
                    Document(
                        mapOf(
                            "seedKey" to item.seedKey,
                            "slug" to item.slug,
                            "name" to item.name,
                            "description" to item.description,
                        ),
                    )
                samplesCollection.replaceOne(
                    Filters.eq("seedKey", item.seedKey),
                    doc,
                    ReplaceOptions().upsert(true),
                )
            }
            logger.info("Seeded ${seedItems.size} sample items successfully.")
        }
    }

    private fun loadSeedSamples(): List<SeedSampleItem> {
        val stream = javaClass.classLoader.getResourceAsStream("seed/samples.json")
        return if (stream != null) {
            val text = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<SeedSampleItem>>(text)
        } else {
            listOf(
                SeedSampleItem(
                    seedKey = "wzorcowa-innowacja",
                    slug = "wzorcowa-innowacja",
                    name = "Wzorcowa Innowacja Społeczna",
                    description = "Przykładowa innowacja seedowa",
                ),
            )
        }
    }
}
