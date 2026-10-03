package io.github.mfabisiak.hubmi

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.plugins.MongoIndexes
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.*

class IdempotentStartupTest {
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
    fun doubleStartupExecutesWithoutErrors() =
        runBlocking {
            val database = client.getDatabase("test-idempotent-startup")
            database.drop()
            val config =
                AppConfig(
                    port = 8080,
                    keycloakIssuer = "issuer",
                    keycloakJwksUrl = "jwks",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-idempotent-startup",
                    seed = true,
                )
            val seeder = DatabaseSeeder(database, config)

            // First startup
            val indexes1 = MongoIndexes.configure(database)
            assertTrue(indexes1 is Either.Right, "Pierwsze tworzenie indeksów powinno zakończyć się sukcesem")

            val seed1 = seeder.seedIfNeeded()
            assertTrue(seed1 is Either.Right, "Pierwsze seedowanie powinno zakończyć się sukcesem")

            // Second startup (simulating restart)
            val indexes2 = MongoIndexes.configure(database)
            assertTrue(indexes2 is Either.Right, "Drugie tworzenie indeksów powinno być w pełni idempotentne")

            val seed2 = seeder.seedIfNeeded()
            assertTrue(seed2 is Either.Right, "Drugie seedowanie powinno być w pełni idempotentne")
        }
}
