package io.github.mfabisiak.hubmi

import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.Api
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.Matches
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

            assertEquals(HttpStatusCode.NotImplemented, client.get(Calls()).status)
            assertEquals(HttpStatusCode.NotImplemented, client.get(Calls.Active()).status)
            assertEquals(HttpStatusCode.NotImplemented, client.post(Matches()).status)
        }

    @Test
    fun queryParametersAreParsedFromTheContract() =
        testApplication {
            application { module() }
            val client = createClient { install(Resources) }

            val response = client.get(Calls(status = CallStatus.OPEN))

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
