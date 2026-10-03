package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.calls.GrantCallItem
import io.github.mfabisiak.hubmi.calls.grantCalls
import io.github.mfabisiak.hubmi.challenges.ChallengeItem
import io.github.mfabisiak.hubmi.challenges.challenges
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.catching
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.ideas.IdeaItem
import io.github.mfabisiak.hubmi.ideas.ideas
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.innovations
import io.github.mfabisiak.hubmi.materials.MaterialItem
import io.github.mfabisiak.hubmi.materials.materials
import io.github.mfabisiak.hubmi.samples.SampleItem
import io.github.mfabisiak.hubmi.samples.samples
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val SEED_USER_ID = "c0000000-0000-0000-0000-000000000001"

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
    val innovativeness: String? = null,
    val problemDiagnosis: String? = null,
    val audienceDescription: String? = null,
    val expectedChange: String? = null,
    val futureVision: String? = null,
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

        logger.info("Running database seeders (SEED=true)...")
        return catching {
            val now = Instant.now().toString()
            seedSamples()
            seedInnovations(now)
            seedChallenges(now)
            seedMaterials(now)
            seedGrantCalls()
            seedIdeas()
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
                Filters.eq(InnovationItem::id, seedObjectId(item.slug)),
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
                    Updates.setOnInsert(InnovationItem::innovativeness, item.innovativeness),
                    Updates.setOnInsert(InnovationItem::problemDiagnosis, item.problemDiagnosis),
                    Updates.setOnInsert(InnovationItem::audienceDescription, item.audienceDescription),
                    Updates.setOnInsert(InnovationItem::expectedChange, item.expectedChange),
                    Updates.setOnInsert(InnovationItem::futureVision, item.futureVision),
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
                Filters.eq(ChallengeItem::id, seedObjectId(item.slug)),
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
                Filters.eq(MaterialItem::id, seedObjectId(item.slug)),
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

    private suspend fun seedGrantCalls() {
        val now = Instant.now()
        val calls =
            listOf(
                GrantCallItem(
                    title = "Małopolski Inkubator Innowacji Społecznych – Nabór 2026",
                    description = "Granty na rozwój i testowanie nowatorskich mikroinnowacji społecznych w Małopolsce.",
                    opensAt = now.minus(14, ChronoUnit.DAYS),
                    closesAt = now.plus(45, ChronoUnit.DAYS),
                    fields =
                        listOf(
                            CallField(
                                key = "opis_problemu",
                                label = "Opis problemu społecznego",
                                required = true,
                            ),
                            CallField(
                                key = "grupa_docelowa",
                                label = "Odbiorcy innowacji",
                                required = true,
                            ),
                            CallField(
                                key = "budzet",
                                label = "Szacowany budżet (PLN)",
                                required = true,
                            ),
                            CallField(
                                key = "partnerzy",
                                label = "Potencjalni partnerzy",
                                required = false,
                            ),
                        ),
                    createdAt = now,
                    updatedAt = now,
                ),
                GrantCallItem(
                    title = "Wsparcie Samodzielności Seniorów – Edycja Wiosna 2027",
                    description = "Inicjatywy wspierające aktywność i niezależność seniorów w środowisku lokalnym.",
                    opensAt = now.plus(30, ChronoUnit.DAYS),
                    closesAt = now.plus(90, ChronoUnit.DAYS),
                    fields =
                        listOf(
                            CallField(
                                key = "tytul",
                                label = "Tytuł projektu",
                                required = true,
                            ),
                            CallField(
                                key = "zalozenia",
                                label = "Główne założenia usługi opiekuńczej",
                                required = true,
                            ),
                        ),
                    createdAt = now,
                    updatedAt = now,
                ),
                GrantCallItem(
                    title = "Pilotaż Innowacji Włączających – 2025",
                    description =
                        "Zakończony nabór pilotażowy projektów włączenia społecznego osób z niepełnosprawnościami.",
                    opensAt = now.minus(180, ChronoUnit.DAYS),
                    closesAt = now.minus(30, ChronoUnit.DAYS),
                    fields =
                        listOf(
                            CallField(
                                key = "podsumowanie",
                                label = "Podsumowanie rezultatów",
                                required = true,
                            ),
                        ),
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        calls.forEach { call ->
            database.grantCalls.updateOne(
                Filters.eq(GrantCallItem::title, call.title),
                Updates.combine(
                    Updates.set(GrantCallItem::description, call.description),
                    Updates.set(GrantCallItem::opensAt, call.opensAt),
                    Updates.set(GrantCallItem::closesAt, call.closesAt),
                    Updates.set(GrantCallItem::fields, call.fields),
                    Updates.set(GrantCallItem::updatedAt, call.updatedAt),
                    Updates.setOnInsert(GrantCallItem::createdAt, call.createdAt),
                ),
                UpdateOptions().upsert(true),
            )
        }
    }

    private suspend fun seedIdeas() {
        val now = Instant.now()
        val ideas =
            listOf(
                IdeaItem(
                    authorId = SEED_USER_ID,
                    title = "Mobilny Asystent Seniora",
                    essence =
                        "Aplikacja łącząca wolontariuszy z seniorami potrzebującymi wsparcia w codziennych sprawach.",
                    targetGroups = listOf(TargetGroup.SENIORS),
                    stage = InnovationStage.PILOT,
                    status = IdeaStatus.SUBMITTED,
                    createdAt = now,
                    updatedAt = now,
                ),
                IdeaItem(
                    authorId = SEED_USER_ID,
                    title = "Centrum Równych Szans",
                    essence =
                        "Klub rówieśniczy wspierający młodzież z obszarów wiejskich w rozwijaniu pasji i integracji.",
                    targetGroups = listOf(TargetGroup.YOUTH),
                    stage = InnovationStage.IDEA,
                    status = IdeaStatus.IN_REVIEW,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        ideas.forEach { idea ->
            database.ideas.updateOne(
                Filters.and(
                    Filters.eq(IdeaItem::authorId, idea.authorId),
                    Filters.eq(IdeaItem::title, idea.title),
                ),
                Updates.combine(
                    Updates.set(IdeaItem::essence, idea.essence),
                    Updates.set(IdeaItem::targetGroups, idea.targetGroups),
                    Updates.set(IdeaItem::stage, idea.stage),
                    Updates.set(IdeaItem::status, idea.status),
                    Updates.set(IdeaItem::updatedAt, idea.updatedAt),
                    Updates.setOnInsert(IdeaItem::createdAt, idea.createdAt),
                ),
                UpdateOptions().upsert(true),
            )
        }
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
