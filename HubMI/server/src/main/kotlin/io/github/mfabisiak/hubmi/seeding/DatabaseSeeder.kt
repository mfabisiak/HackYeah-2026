package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.models.ChallengeItem
import io.github.mfabisiak.hubmi.models.InnovationItem
import io.github.mfabisiak.hubmi.models.MaterialItem
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.catching
import io.github.mfabisiak.hubmi.repository.challenges
import io.github.mfabisiak.hubmi.repository.innovations
import io.github.mfabisiak.hubmi.repository.materials
import io.github.mfabisiak.hubmi.repository.samples
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant

@Serializable
data class SeedSampleItem(
    val slug: String,
    val name: String,
    val description: String,
)

@Serializable
data class SeedInnovationItem(
    val slug: String,
    val title: String,
    val summary: String,
    val description: String,
    val areas: List<SocialArea>,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val region: String? = null,
    val keywords: List<String> = emptyList(),
    val mediaUrls: List<String> = emptyList(),
)

@Serializable
data class SeedChallengeItem(
    val slug: String,
    val title: String,
    val description: String,
    val area: SocialArea,
    val municipalities: List<String> = emptyList(),
)

@Serializable
data class SeedMaterialItem(
    val slug: String,
    val title: String,
    val description: String,
    val type: MaterialType,
    val url: String,
    val areas: List<SocialArea> = emptyList(),
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
            val now = Instant.now().toString()
            seedSamples()
            seedInnovations(now)
            seedChallenges(now)
            seedMaterials(now)
            logger.info("Database seeding completed successfully.")
        }
    }

    private suspend fun seedSamples() {
        val seedItems = loadFromResource<SeedSampleItem>("seed/samples.json")
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
        logger.info("Seeded ${seedItems.size} sample items.")
    }

    private suspend fun seedInnovations(now: String) {
        val seedItems = loadFromResource<SeedInnovationItem>("seed/innovations.json")
        seedItems.forEach { item ->
            database.innovations.updateOne(
                Filters.eq(InnovationItem::slug, item.slug),
                Updates.combine(
                    Updates.set(InnovationItem::slug, item.slug),
                    Updates.set(InnovationItem::title, item.title),
                    Updates.set(InnovationItem::summary, item.summary),
                    Updates.set(InnovationItem::description, item.description),
                    Updates.set(InnovationItem::areas, item.areas),
                    Updates.set(InnovationItem::targetGroups, item.targetGroups),
                    Updates.set(InnovationItem::stage, item.stage),
                    Updates.set(InnovationItem::region, item.region),
                    Updates.set(InnovationItem::keywords, item.keywords),
                    Updates.set(InnovationItem::mediaUrls, item.mediaUrls),
                    Updates.setOnInsert(InnovationItem::ratingSum, 0),
                    Updates.setOnInsert(InnovationItem::ratingsCount, 0),
                    Updates.setOnInsert(InnovationItem::archived, false),
                    Updates.setOnInsert(InnovationItem::createdAt, now),
                    Updates.set(InnovationItem::updatedAt, now),
                ),
                UpdateOptions().upsert(true),
            )
        }
        logger.info("Seeded ${seedItems.size} innovation items.")
    }

    private suspend fun seedChallenges(now: String) {
        val seedItems = loadFromResource<SeedChallengeItem>("seed/challenges.json")
        seedItems.forEach { item ->
            database.challenges.updateOne(
                Filters.eq(ChallengeItem::slug, item.slug),
                Updates.combine(
                    Updates.set(ChallengeItem::slug, item.slug),
                    Updates.set(ChallengeItem::title, item.title),
                    Updates.set(ChallengeItem::description, item.description),
                    Updates.set(ChallengeItem::area, item.area),
                    Updates.set(ChallengeItem::municipalities, item.municipalities),
                    Updates.setOnInsert(ChallengeItem::archived, false),
                    Updates.setOnInsert(ChallengeItem::createdAt, now),
                    Updates.set(ChallengeItem::updatedAt, now),
                ),
                UpdateOptions().upsert(true),
            )
        }
        logger.info("Seeded ${seedItems.size} challenge items.")
    }

    private suspend fun seedMaterials(now: String) {
        val seedItems = loadFromResource<SeedMaterialItem>("seed/materials.json")
        seedItems.forEach { item ->
            database.materials.updateOne(
                Filters.eq(MaterialItem::slug, item.slug),
                Updates.combine(
                    Updates.set(MaterialItem::slug, item.slug),
                    Updates.set(MaterialItem::title, item.title),
                    Updates.set(MaterialItem::description, item.description),
                    Updates.set(MaterialItem::type, item.type),
                    Updates.set(MaterialItem::url, item.url),
                    Updates.set(MaterialItem::areas, item.areas),
                    Updates.setOnInsert(MaterialItem::archived, false),
                    Updates.setOnInsert(MaterialItem::createdAt, now),
                    Updates.set(MaterialItem::updatedAt, now),
                ),
                UpdateOptions().upsert(true),
            )
        }
        logger.info("Seeded ${seedItems.size} material items.")
    }

    private inline fun <reified T> loadFromResource(resourcePath: String): List<T> {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
        return if (stream != null) {
            val text = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<T>>(text)
        } else {
            emptyList()
        }
    }
}
