package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.round

/**
 * Embeddings recorded from a real Ollama and keyed by the hash of the text, so the hybrid engine can be measured and
 * tested in CI without a model. Re-record after changing a seeded innovation or the golden set:
 * `WRITE_MATCHING_EMBEDDINGS=true ./gradlew :server:test --tests '*RecordEmbeddingsTest*'` (needs `ollama` with the model).
 */
@Serializable
data class RecordedEmbeddings(
    val model: String,
    val vectors: Map<String, List<Float>>,
) : EmbeddingClient {
    override suspend fun embed(texts: List<String>): Either<EmbeddingError, List<Embedding>> =
        texts
            .map { text -> vectors[hash(text)]?.let(::Embedding) }
            .let { found ->
                if (found.all { it != null }) {
                    found.filterNotNull().right()
                } else {
                    EmbeddingError
                        .InvalidResponse("Brak nagranego wektora; nagraj ponownie (zob. RecordedEmbeddings)")
                        .left()
                }
            }

    fun write(file: File) {
        GZIPOutputStream(file.outputStream()).use { it.write(json.encodeToString(this).toByteArray()) }
    }

    companion object {
        const val RESOURCE = "matching/embeddings.json.gz"
        private val json = Json

        fun hash(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

        /** Four decimals keep the cosine exact to ~1e-4 and the file small. */
        fun rounded(values: List<Float>): List<Float> = values.map { round(it * 10_000f) / 10_000f }

        fun load(): RecordedEmbeddings =
            GZIPInputStream(RecordedEmbeddings::class.java.classLoader.getResourceAsStream(RESOURCE)).use {
                json.decodeFromString(it.readBytes().decodeToString())
            }
    }
}
