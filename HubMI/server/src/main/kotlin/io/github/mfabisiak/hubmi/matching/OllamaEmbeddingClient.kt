package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/** Embeddings from a local Ollama (`POST /api/embed`); nothing leaves the machine. */
class OllamaEmbeddingClient(
    private val http: HttpClient,
    baseUrl: String,
    private val model: String,
) : EmbeddingClient {
    @Serializable
    private data class Request(
        val model: String,
        val input: List<String>,
    )

    @Serializable
    private data class Response(
        val embeddings: List<List<Float>>,
    )

    private val endpoint = "${baseUrl.trimEnd('/')}/api/embed"

    override suspend fun embed(texts: List<String>): Either<EmbeddingError, List<Embedding>> =
        either {
            val response =
                Either
                    .catch {
                        http.post(endpoint) {
                            contentType(ContentType.Application.Json)
                            setBody(Request(model, texts))
                        }
                    }.mapLeft { EmbeddingError.Unavailable(cause = it) }
                    .bind()
            ensure(response.status.isSuccess()) {
                EmbeddingError.Unavailable(detail = "Ollama odpowiedziała statusem ${response.status.value}")
            }
            val vectors = parse(response).bind().embeddings
            ensure(vectors.size == texts.size && vectors.all { it.isNotEmpty() }) {
                EmbeddingError.InvalidResponse("Oczekiwano ${texts.size} niepustych wektorów, jest ${vectors.size}")
            }
            vectors.map(::Embedding)
        }

    private suspend fun parse(response: HttpResponse): Either<EmbeddingError, Response> =
        Either.catch { response.body<Response>() }.mapLeft { EmbeddingError.InvalidResponse(it.message.orEmpty()) }
}
