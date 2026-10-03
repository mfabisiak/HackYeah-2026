package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.challenges.challenges
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.innovations
import io.github.mfabisiak.hubmi.materials.materials
import io.github.mfabisiak.hubmi.samples.SampleItem
import io.github.mfabisiak.hubmi.samples.samples
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.*

class DatabaseSeederTest {
    companion object {
        private lateinit var client: MongoClient

        @BeforeClass
        @JvmStatic
        fun setUp() {
            client = MongoClient.create(MongoTestEnvironment.connectionString)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            client.close()
        }
    }

    @Test
    fun seedDisabledDoesNotInsertData() =
        runBlocking {
            val database = client.getDatabase("test-seeder-disabled")
            database.drop()
            val config =
                AppConfig(
                    port = 8080,
                    keycloakIssuer = "issuer",
                    keycloakJwksUrl = "jwks",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-seeder-disabled",
                    seed = false,
                )
            val seeder = DatabaseSeeder(database, config)
            val result = seeder.seedIfNeeded()
            assertTrue(result is Either.Right)

            val count = database.samples.countDocuments()
            assertEquals(0L, count)
            assertEquals(0L, database.innovations.countDocuments())
            assertEquals(0L, database.challenges.countDocuments())
            assertEquals(0L, database.materials.countDocuments())
        }

    @Test
    fun seedEnabledInsertsDataIdempotently() =
        runBlocking {
            val database = client.getDatabase("test-seeder-enabled")
            database.drop()
            val config =
                AppConfig(
                    port = 8080,
                    keycloakIssuer = "issuer",
                    keycloakJwksUrl = "jwks",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-seeder-enabled",
                    seed = true,
                )
            val seeder = DatabaseSeeder(database, config)

            // First run
            val result1 = seeder.seedIfNeeded()
            assertTrue(result1 is Either.Right)

            val samplesCount1 = database.samples.countDocuments()
            assertTrue(samplesCount1 >= 1L)

            val innovationsCount1 = database.innovations.countDocuments()
            assertTrue(
                innovationsCount1 in 30L..50L,
                "Innovations count should be between 30 and 50 (got $innovationsCount1)",
            )

            val challengesCount1 = database.challenges.countDocuments()
            assertTrue(
                challengesCount1 >= 10L,
                "Challenges count should be at least 10 (got $challengesCount1)",
            )

            val materialsCount1 = database.materials.countDocuments()
            assertTrue(
                materialsCount1 >= 15L,
                "Materials count should be at least 15 (got $materialsCount1)",
            )

            // Verify taxonomy coverage in innovations
            val allInnovations = database.innovations.find().toList()
            val coveredAreas = allInnovations.flatMap { it.areas }.toSet()
            val coveredTargetGroups = allInnovations.flatMap { it.targetGroups }.toSet()
            val coveredStages = allInnovations.map { it.stage }.toSet()

            for (area in SocialArea.entries) {
                assertTrue(area in coveredAreas, "SocialArea $area should be covered in seeded innovations")
            }
            for (tg in TargetGroup.entries) {
                assertTrue(tg in coveredTargetGroups, "TargetGroup $tg should be covered in seeded innovations")
            }
            for (stage in InnovationStage.entries) {
                assertTrue(stage in coveredStages, "InnovationStage $stage should be covered in seeded innovations")
            }

            // Every seeded innovation carries the narrative sections of ROPS' application form
            allInnovations.forEach { item ->
                listOf(
                    item.innovativeness,
                    item.problemDiagnosis,
                    item.audienceDescription,
                    item.expectedChange,
                    item.futureVision,
                ).forEach { section -> assertTrue(!section.isNullOrBlank(), "Missing section in ${item.title}") }
            }

            // Second run (idempotent upsert)
            val result2 = seeder.seedIfNeeded()
            assertTrue(result2 is Either.Right)
            assertEquals(samplesCount1, database.samples.countDocuments())
            assertEquals(innovationsCount1, database.innovations.countDocuments())
            assertEquals(challengesCount1, database.challenges.countDocuments())
            assertEquals(materialsCount1, database.materials.countDocuments())

            val sample =
                database.samples
                    .find(Filters.eq(SampleItem::slug, "wzorcowa-innowacja"))
                    .toList()
                    .firstOrNull()
            assertNotNull(sample)
            assertEquals("Wzorcowa Innowacja Społeczna", sample.name)
        }

    @Test
    fun reseedingKeepsAdminEditsAndArchivedItems() =
        runBlocking {
            val database = client.getDatabase("test-seeder-keeps-edits")
            database.drop()
            val config =
                AppConfig(
                    port = 8080,
                    keycloakIssuer = "issuer",
                    keycloakJwksUrl = "jwks",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-seeder-keeps-edits",
                    seed = true,
                )
            val seeder = DatabaseSeeder(database, config)
            seeder.seedIfNeeded()

            val (edited, archived) =
                database.innovations
                    .find()
                    .toList()
                    .take(2)
            database.innovations.updateOne(
                Filters.eq(InnovationItem::id, edited.id),
                Updates.set(InnovationItem::title, "Zmieniony przez admina"),
            )
            database.innovations.updateOne(
                Filters.eq(InnovationItem::id, archived.id),
                Updates.set(InnovationItem::archived, true),
            )

            assertTrue(seeder.seedIfNeeded() is Either.Right)

            val after =
                database.innovations
                    .find()
                    .toList()
                    .associateBy(InnovationItem::id)
            assertEquals("Zmieniony przez admina", after.getValue(edited.id).title)
            assertTrue(after.getValue(archived.id).archived)
            assertEquals(edited.createdAt, after.getValue(edited.id).createdAt)
        }
}
