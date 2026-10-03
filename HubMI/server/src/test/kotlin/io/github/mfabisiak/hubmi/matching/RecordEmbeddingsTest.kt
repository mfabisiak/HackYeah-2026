package io.github.mfabisiak.hubmi.matching

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records the vectors the hybrid tests run on. Does nothing unless `WRITE_MATCHING_EMBEDDINGS=true`. */
class RecordEmbeddingsTest {
    @Test
    fun recordEmbeddings() {
        if (System.getenv("WRITE_MATCHING_EMBEDDINGS") != "true") return
        val model = System.getenv("EMBEDDING_MODEL") ?: "bge-m3"
        val texts =
            (MatchingHarness.seed.map { it.embeddingText() } + MatchingHarness.golden.map { it.query })
                .distinct()
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 120_000 }
        }.use { http ->
            val client = OllamaEmbeddingClient(http, System.getenv("OLLAMA_URL") ?: "http://localhost:11434", model)
            val embeddings = runBlocking { client.embed(texts) }.getOrNull()
            assertTrue(embeddings != null, "Ollama did not answer; is it running with the $model model?")
            assertEquals(texts.size, embeddings.size)
            RecordedEmbeddings(
                model = model,
                vectors =
                    texts
                        .zip(embeddings) { text, e ->
                            RecordedEmbeddings.hash(text) to
                                RecordedEmbeddings.rounded(e.values)
                        }.toMap(),
            ).write(File("src/test/resources/${RecordedEmbeddings.RESOURCE}"))
        }
    }
}
