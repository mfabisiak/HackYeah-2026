package io.github.mfabisiak.hubmi.contract

import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.Api
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.module
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ContractStubsTest {
    @Test
    fun publicStubsAnswerNotImplemented() =
        testApplication {
            application { module() }
            val client = createClient { install(Resources) }

            assertEquals(HttpStatusCode.NotImplemented, client.post(Matches()).status)
            assertEquals(HttpStatusCode.NotImplemented, client.post(Matches.Feedback(needId = "abc")).status)
        }

    @Test
    fun stubAnswersWithNotImplementedErrorCode() =
        testApplication {
            application { module() }
            val client = createClient { install(Resources) }

            val response = client.post(Matches())

            assertEquals(HttpStatusCode.NotImplemented, response.status)
            assertTrue(response.bodyAsText().contains(ErrorCode.NOT_IMPLEMENTED.name))
        }

    @Test
    fun authenticatedStubsRequireToken() =
        testApplication {
            application { module() }
            val client = createClient { install(Resources) }

            assertEquals(HttpStatusCode.Unauthorized, client.post(Ideas()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(Ideas.Mine()).status)
        }

    @Test
    fun adminStubsRequireToken() =
        testApplication {
            application { module() }
            val client = createClient { install(Resources) }

            assertEquals(HttpStatusCode.Unauthorized, client.get(AdminTrends()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.post(Calls()).status)
            assertEquals(HttpStatusCode.Unauthorized, client.delete(Calls.ById(id = "abc")).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(Api.Me()).status)
        }
}
