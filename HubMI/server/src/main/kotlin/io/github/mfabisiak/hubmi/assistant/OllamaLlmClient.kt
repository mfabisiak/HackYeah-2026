package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readLine
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * A local Ollama (`POST /api/chat`, streamed as one JSON object per line); nothing leaves the machine. The answer is
 * constrained to the request's JSON Schema through Ollama's `format`.
 */
class OllamaLlmClient(
    private val http: HttpClient,
    baseUrl: String,
    private val model: String,
    private val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
) : LlmClient {
    @Serializable
    private data class Message(
        val role: String,
        val content: String,
    )

    @Serializable
    private data class Options(
        val temperature: Double,
        @SerialName("num_predict") val numPredict: Int,
        @SerialName("num_ctx") val numCtx: Int,
    )

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val format: JsonObject,
        val options: Options,
        @SerialName("keep_alive") val keepAlive: String,
        val stream: Boolean = true,
    )

    @Serializable
    private data class ChatLine(
        val message: Message? = null,
        val done: Boolean = false,
        val error: String? = null,
    )

    @Serializable
    private data class LoadRequest(
        val model: String,
        @SerialName("keep_alive") val keepAlive: String,
    )

    private val chatEndpoint = "${baseUrl.trimEnd('/')}/api/chat"
    private val generateEndpoint = "${baseUrl.trimEnd('/')}/api/generate"
    private val lenient = Json { ignoreUnknownKeys = true }

    override fun stream(request: LlmRequest): Flow<Either<LlmError, String>> =
        channelFlow {
            val body =
                ChatRequest(
                    model = model,
                    messages = listOf(Message(SYSTEM_ROLE, request.system), Message(USER_ROLE, request.user)),
                    format = request.schema,
                    options = Options(request.temperature, request.maxTokens, CONTEXT_TOKENS),
                    keepAlive = KEEP_ALIVE,
                )
            Either
                .catch {
                    http
                        .preparePost(chatEndpoint) {
                            contentType(ContentType.Application.Json)
                            setBody(body)
                            timeout { requestTimeoutMillis = this@OllamaLlmClient.requestTimeoutMillis }
                        }.execute { response -> relay(response) }
                }.fold(
                    ifLeft = { LlmError.Unavailable(cause = it).left() },
                    ifRight = { it },
                ).onLeft { send(it.left()) }
        }

    override suspend fun warmUp(): Either<LlmError, Unit> =
        either {
            val response =
                Either
                    .catch {
                        http.post(generateEndpoint) {
                            contentType(ContentType.Application.Json)
                            setBody(LoadRequest(model, KEEP_ALIVE))
                        }
                    }.mapLeft { LlmError.Unavailable(cause = it) }
                    .bind()
            ensure(response.status.isSuccess()) { unexpectedStatus(response) }
        }

    private suspend fun ProducerScope<Either<LlmError, String>>.relay(response: HttpResponse): Either<LlmError, Unit> =
        if (response.status.isSuccess()) relayLines(response.bodyAsChannel()) else unexpectedStatus(response).left()

    /** Sends the text of each line; succeeds on the line marked `done`, fails if the answer breaks off or is not Ollama's. */
    private tailrec suspend fun ProducerScope<Either<LlmError, String>>.relayLines(
        channel: ByteReadChannel,
    ): Either<LlmError, Unit> {
        val line = channel.readLine() ?: return LlmError.InvalidResponse("Odpowiedź urwana przed końcem").left()
        if (line.isBlank()) return relayLines(channel)
        val chat = decode(line).getOrElse { return it.left() }
        chat.error?.let { return LlmError.Unavailable(detail = it).left() }
        chat.message
            ?.content
            ?.takeIf(String::isNotEmpty)
            ?.let { send(it.right()) }
        return if (chat.done) Unit.right() else relayLines(channel)
    }

    private fun decode(line: String): Either<LlmError, ChatLine> =
        Either
            .catch { lenient.decodeFromString<ChatLine>(line) }
            .mapLeft { LlmError.InvalidResponse(it.message.orEmpty()) }

    private fun unexpectedStatus(response: HttpResponse) =
        LlmError.Unavailable(detail = "Ollama odpowiedziała statusem ${response.status.value}")

    private companion object {
        const val SYSTEM_ROLE = "system"
        const val USER_ROLE = "user"
        const val KEEP_ALIVE = "30m"
        const val CONTEXT_TOKENS = 4096
        const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 60_000L
    }
}
