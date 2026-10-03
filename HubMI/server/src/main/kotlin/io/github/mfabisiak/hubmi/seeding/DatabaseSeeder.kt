package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.CallFieldType
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.models.GrantCallItem
import io.github.mfabisiak.hubmi.models.IdeaItem
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.catching
import io.github.mfabisiak.hubmi.repository.grantCalls
import io.github.mfabisiak.hubmi.repository.ideas
import io.github.mfabisiak.hubmi.repository.samples
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

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

        logger.info("Running database seeders (SEED=true)...")
        return catching {
            seedSamples()
            seedGrantCalls()
            seedIdeas()
            logger.info("Database seeding completed successfully.")
        }
    }

    private suspend fun seedSamples() {
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
                                type = CallFieldType.LONG_TEXT,
                                helpText = "Jaki problem w Małopolsce rozwiązuje innowacja?",
                                prefillFromIdea = "essence",
                            ),
                            CallField(
                                key = "grupa_docelowa",
                                label = "Odbiorcy innowacji",
                                required = true,
                                type = CallFieldType.TEXT,
                                helpText = "Do kogo bezpośrednio skierowane jest wsparcie?",
                            ),
                            CallField(
                                key = "budzet",
                                label = "Szacowany budżet (PLN)",
                                required = true,
                                type = CallFieldType.NUMBER,
                            ),
                            CallField(
                                key = "partnerzy",
                                label = "Potencjalni partnerzy",
                                required = false,
                                type = CallFieldType.LONG_TEXT,
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
                                type = CallFieldType.TEXT,
                            ),
                            CallField(
                                key = "zalozenia",
                                label = "Główne założenia usługi opiekuńczej",
                                required = true,
                                type = CallFieldType.LONG_TEXT,
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
                                type = CallFieldType.LONG_TEXT,
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
                    authorId = "user",
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
                    authorId = "user",
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
