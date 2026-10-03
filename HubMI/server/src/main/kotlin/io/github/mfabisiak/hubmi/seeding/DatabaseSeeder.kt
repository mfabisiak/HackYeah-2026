package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.catching
import io.github.mfabisiak.hubmi.repository.samples
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class SeedSampleItem(
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
            val seedItems = loadSeedSamples()

            seedItems.forEach { item ->
                database.samples.updateOne(
                    Filters.eq(SampleItem::slug, item.slug),
                    Updates.combine(
                        Updates.set(SampleItem::name, item.name),
                        Updates.set(SampleItem::description, item.description),
                    ),
                    UpdateOptions().upsert(true),
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
                    slug = "wzorcowa-innowacja",
                    name = "Wzorcowa Innowacja Społeczna",
                    description = "Przykładowa innowacja seedowa",
                ),
            )
        }
    }
}
