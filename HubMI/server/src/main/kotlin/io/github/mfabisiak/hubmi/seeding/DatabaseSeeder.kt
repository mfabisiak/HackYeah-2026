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
import io.github.mfabisiak.hubmi.challenges.ChallengeItem
import io.github.mfabisiak.hubmi.challenges.challenges
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.catching
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.innovations
import io.github.mfabisiak.hubmi.materials.MaterialItem
import io.github.mfabisiak.hubmi.materials.materials
import io.github.mfabisiak.hubmi.samples.SampleItem
import io.github.mfabisiak.hubmi.samples.samples
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Instant

private const val OBJECT_ID_BYTES = 12

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
                Filters.eq(InnovationItem::id, seedId(item.slug)),
                Updates.combine(
                    Updates.setOnInsert(InnovationItem::title, item.title),
                    Updates.setOnInsert(InnovationItem::summary, item.summary),
                    Updates.setOnInsert(InnovationItem::description, item.description),
                    Updates.setOnInsert(InnovationItem::areas, item.areas),
                    Updates.setOnInsert(InnovationItem::targetGroups, item.targetGroups),
                    Updates.setOnInsert(InnovationItem::stage, item.stage),
                    Updates.setOnInsert(InnovationItem::region, item.region),
                    Updates.setOnInsert(InnovationItem::keywords, item.keywords),
                    Updates.setOnInsert(InnovationItem::mediaUrls, item.mediaUrls),
                    Updates.setOnInsert(InnovationItem::ratingSum, 0),
                    Updates.setOnInsert(InnovationItem::ratingsCount, 0),
                    Updates.setOnInsert(InnovationItem::archived, false),
                    Updates.setOnInsert(InnovationItem::createdAt, now),
                    Updates.setOnInsert(InnovationItem::updatedAt, now),
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
                Filters.eq(ChallengeItem::id, seedId(item.slug)),
                Updates.combine(
                    Updates.setOnInsert(ChallengeItem::title, item.title),
                    Updates.setOnInsert(ChallengeItem::description, item.description),
                    Updates.setOnInsert(ChallengeItem::area, item.area),
                    Updates.setOnInsert(ChallengeItem::municipalities, item.municipalities),
                    Updates.setOnInsert(ChallengeItem::archived, false),
                    Updates.setOnInsert(ChallengeItem::createdAt, now),
                    Updates.setOnInsert(ChallengeItem::updatedAt, now),
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
                Filters.eq(MaterialItem::id, seedId(item.slug)),
                Updates.combine(
                    Updates.setOnInsert(MaterialItem::title, item.title),
                    Updates.setOnInsert(MaterialItem::description, item.description),
                    Updates.setOnInsert(MaterialItem::type, item.type),
                    Updates.setOnInsert(MaterialItem::url, item.url),
                    Updates.setOnInsert(MaterialItem::areas, item.areas),
                    Updates.setOnInsert(MaterialItem::archived, false),
                    Updates.setOnInsert(MaterialItem::createdAt, now),
                    Updates.setOnInsert(MaterialItem::updatedAt, now),
                ),
                UpdateOptions().upsert(true),
            )
        }
        logger.info("Seeded ${seedItems.size} material items.")
    }

    /**
     * The slug lives only in the seed files: it is hashed into a stable `_id`, so re-running the seeder upserts the same
     * documents without a technical key in the model. `$setOnInsert` keeps edits and deletions made by admins.
     */
    private fun seedId(slug: String): ObjectId =
        ObjectId(MessageDigest.getInstance("SHA-1").digest(slug.toByteArray()).copyOf(OBJECT_ID_BYTES))

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
