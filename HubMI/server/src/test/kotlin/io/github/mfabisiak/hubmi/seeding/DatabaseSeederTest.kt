package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.model.Filters
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.samples
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
            val count1 = database.samples.countDocuments()
            assertTrue(count1 >= 2L)

            // Second run (idempotent upsert)
            val result2 = seeder.seedIfNeeded()
            assertTrue(result2 is Either.Right)
            val count2 = database.samples.countDocuments()
            assertEquals(count1, count2)

            val sample =
                database.samples
                    .find(Filters.eq(SampleItem::slug, "wzorcowa-innowacja"))
                    .toList()
                    .firstOrNull()
            assertNotNull(sample)
            assertEquals("Wzorcowa Innowacja Społeczna", sample.name)
        }
}
