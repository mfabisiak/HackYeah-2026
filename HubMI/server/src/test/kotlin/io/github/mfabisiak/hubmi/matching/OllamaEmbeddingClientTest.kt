package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OllamaEmbeddingClientTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun client(engine: MockEngine) =
        OllamaEmbeddingClient(
            http =
                HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            baseUrl = "http://ollama:11434/",
            model = "bge-m3",
        )

    private fun embed(
        engine: MockEngine,
        vararg texts: String,
    ) = runBlocking { client(engine).embed(texts.toList()) }

    @Test
    fun sendsAllTextsInOneRequestAndKeepsTheirOrder() {
        val engine =
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("http://ollama:11434/api/embed", request.url.toString())
                assertEquals(
                    """{"model":"bge-m3","input":["a","b"]}""",
                    String(request.body.toByteArray()),
                )
                respond(
                    """{"model":"bge-m3","embeddings":[[1.0,0.0],[0.0,1.0]],"total_duration":5}""",
                    headers = jsonHeaders,
                )
            }

        val result = embed(engine, "a", "b")

        assertEquals(
            Either.Right(listOf(Embedding(listOf(1f, 0f)), Embedding(listOf(0f, 1f)))),
            result,
        )
    }

    @Test
    fun errorStatusIsUnavailable() {
        val result = embed(MockEngine { respondError(HttpStatusCode.InternalServerError) }, "a")

        assertIs<EmbeddingError.Unavailable>(result.leftOrNull())
    }

    @Test
    fun unreachableServerIsUnavailableNotAnException() {
        val result = embed(MockEngine { throw IOException("connection refused") }, "a")

        assertIs<EmbeddingError.Unavailable>(result.leftOrNull())
    }

    @Test
    fun wrongNumberOfVectorsIsInvalid() {
        val result = embed(MockEngine { respond("""{"embeddings":[[1.0]]}""", headers = jsonHeaders) }, "a", "b")

        assertIs<EmbeddingError.InvalidResponse>(result.leftOrNull())
    }

    @Test
    fun emptyVectorIsInvalid() {
        val result = embed(MockEngine { respond("""{"embeddings":[[]]}""", headers = jsonHeaders) }, "a")

        assertIs<EmbeddingError.InvalidResponse>(result.leftOrNull())
    }

    @Test
    fun malformedBodyIsInvalid() {
        val result = embed(MockEngine { respond("""{"unexpected":true}""", headers = jsonHeaders) }, "a")

        assertIs<EmbeddingError.InvalidResponse>(result.leftOrNull())
    }
}
