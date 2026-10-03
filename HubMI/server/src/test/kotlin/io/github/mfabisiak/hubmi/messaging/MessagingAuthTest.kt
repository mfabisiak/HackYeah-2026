package io.github.mfabisiak.hubmi.messaging

import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.module
import io.github.mfabisiak.hubmi.offlineConfig
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class MessagingAuthTest {
    @Test
    fun threadsAndNotificationsRequireToken() =
        testApplication {
            application { module(offlineConfig) }
            val client = createClient { install(Resources) }

            assertEquals(HttpStatusCode.Unauthorized, client.get(Threads()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(Notifications()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.post(Notifications.Read(id = "abc")).status)
        }
}
