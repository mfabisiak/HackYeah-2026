package io.github.mfabisiak.hubmi.contract

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.github.mfabisiak.hubmi.offlineConfig
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import kotlin.test.*

class ContractStubsTest {
    @Test
    fun authenticatedStubsRequireToken() =
        testApplication {
            application { module(offlineConfig) }
            val client = createClient { install(Resources) }

            assertEquals(HttpStatusCode.Unauthorized, client.get(Threads()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(Notifications()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.post(Notifications.Read(id = "abc")).status)
        }

    @Test
    fun adminStubsRequireToken() =
        testApplication {
            application { module(offlineConfig) }
            val client = createClient { install(Resources) }

            assertEquals(HttpStatusCode.Unauthorized, client.get(AdminTrends()).status)
        }
}
