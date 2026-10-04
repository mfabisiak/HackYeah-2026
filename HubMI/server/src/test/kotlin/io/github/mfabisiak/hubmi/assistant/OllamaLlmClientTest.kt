package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OllamaLlmClientTest {
    private val request =
        LlmRequest(
            system = "System",
            user = "Użytkownik",
            schema = buildJsonObject { put("type", "object") },
            maxTokens = 123,
        )

    private fun client(engine: MockEngine) =
        OllamaLlmClient(
            HttpClient(engine) {
                install(ContentNegotiation) { json() }
                install(HttpTimeout)
            },
            "http://ollama:11434/",
            "bielik",
        )

    private fun stream(engine: MockEngine): List<Either<LlmError, String>> =
        runBlocking { client(engine).stream(request).toList() }

    private fun line(
        content: String,
        done: Boolean = false,
    ) = """{"model":"bielik","message":{"role":"assistant","content":"$content"},"done":$done}""" + "\n"

    @Test
    fun theAnswerArrivesPieceByPieceAndTheRequestCarriesTheSchemaAndLimits() {
        val engine =
            MockEngine { received ->
                assertEquals(HttpMethod.Post, received.method)
                assertEquals("http://ollama:11434/api/chat", received.url.toString())
                val body = String(received.body.toByteArray())
                assertTrue("\"model\":\"bielik\"" in body)
                assertTrue("\"format\":{\"type\":\"object\"}" in body)
                assertTrue("\"num_predict\":123" in body)
                assertTrue("\"stream\":true" in body)
                respond(line("{\\\"a\\\"") + line(":1}") + line("", done = true))
            }

        assertEquals(listOf(Either.Right("{\"a\""), Either.Right(":1}")), stream(engine))
    }

    @Test
    fun anErrorStatusIsUnavailable() {
        val result = stream(MockEngine { respondError(HttpStatusCode.NotFound) })

        assertIs<LlmError.Unavailable>(result.single().leftOrNull())
    }

    @Test
    fun anUnreachableOllamaIsUnavailable() {
        val result = stream(MockEngine { throw IOException("connection refused") })

        assertIs<LlmError.Unavailable>(result.single().leftOrNull())
    }

    @Test
    fun anErrorLineEndsTheStreamAfterWhatCameBefore() {
        val result = stream(MockEngine { respond(line("abc") + """{"error":"model requires more memory"}""" + "\n") })

        assertEquals(Either.Right("abc"), result.first())
        assertIs<LlmError.Unavailable>(result.last().leftOrNull())
        assertEquals(2, result.size)
    }

    @Test
    fun anAnswerCutOffBeforeTheDoneLineIsInvalid() {
        val result = stream(MockEngine { respond(line("abc")) })

        assertEquals(Either.Right("abc"), result.first())
        assertIs<LlmError.InvalidResponse>(result.last().leftOrNull())
    }

    @Test
    fun aLineThatIsNotJsonIsInvalid() {
        val result = stream(MockEngine { respond("<html>proxy error</html>\n") })

        assertIs<LlmError.InvalidResponse>(result.single().leftOrNull())
    }
}
