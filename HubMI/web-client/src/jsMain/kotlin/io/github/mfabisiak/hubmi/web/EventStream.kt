package io.github.mfabisiak.hubmi.web

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.mfabisiak.hubmi.api.SseMessage
import io.github.mfabisiak.hubmi.api.SseParser
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.preparePost
import io.ktor.client.request.accept
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readLine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val CANCELLED = ApiErrorJs(CLIENT_ERROR_STATUS, "CANCELLED", "Przerwano")
private val INCOMPLETE =
    ApiErrorJs(CLIENT_ERROR_STATUS, "INVALID_RESPONSE", "Strumień urwał się przed końcem odpowiedzi")
private val json = Json { ignoreUnknownKeys = true }

/**
 * `POST`s [body] to a shared `@Resource` and reads the answer as server-sent events. The browser's `EventSource` cannot
 * send the `Authorization` header, so the stream is read from the `fetch` body and parsed by [SseParser]. A failure
 * (HTTP status, network) is the last element, and nothing follows it; cancelling the collector aborts the request.
 */
internal inline fun <reified R : Any, reified B : Any> HttpClient.eventStream(
    resource: R,
    body: B,
): Flow<Either<ApiErrorJs, SseMessage>> =
    channelFlow {
        Either
            .catch {
                preparePost(resource) {
                    accept(ContentType.Text.EventStream)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status.isSuccess()) {
                        relayMessages(response.bodyAsChannel(), SseParser.INITIAL)
                    } else {
                        send(response.toApiError().left())
                    }
                }
            }.onLeft { send(ApiErrorJs(CLIENT_ERROR_STATUS, "NETWORK_ERROR", it.message ?: "Network error").left()) }
    }

internal tailrec suspend fun ProducerScope<Either<ApiErrorJs, SseMessage>>.relayMessages(
    channel: ByteReadChannel,
    parser: SseParser,
) {
    val line = channel.readLine() ?: return
    val step = parser.accept(line)
    step.message?.let { send(it.right()) }
    relayMessages(channel, step.parser)
}

/** Reads the `data:` of a message as an event of the sealed type that [serializer] belongs to. */
internal fun <E> KSerializer<E>.decoder(): (SseMessage) -> Either<ApiErrorJs, E> =
    { message ->
        Either
            .catch { json.decodeFromString(this, message.data) }
            .mapLeft { ApiErrorJs(CLIENT_ERROR_STATUS, "INVALID_RESPONSE", it.message ?: "Invalid event") }
    }

/**
 * Decodes the messages into events, hands each to [onEvent] and returns the answer that [finish] takes from the closing
 * one (`null` for events that are not the answer, a `Left` for one that reports a failure).
 */
internal suspend fun <E, R> Flow<Either<ApiErrorJs, SseMessage>>.collectEvents(
    decode: (SseMessage) -> Either<ApiErrorJs, E>,
    onEvent: (E) -> Unit,
    finish: (E) -> Either<ApiErrorJs, R>?,
): Either<ApiErrorJs, R> =
    fold<_, Either<ApiErrorJs, R?>>(null.right()) { previous, message ->
        previous.flatMap { answer ->
            message.flatMap(decode).flatMap { event ->
                onEvent(event)
                finish(event) ?: answer.right()
            }
        }
    }.flatMap { it?.right() ?: INCOMPLETE.left() }

/** Runs [call] in the background and returns at once; the promise settles with its result mapped by [transform]. */
internal fun <A, B> CoroutineScope.startStream(
    call: suspend () -> Either<ApiErrorJs, A>,
    transform: (A) -> B,
): StreamJs<B> {
    val outcome = CompletableDeferred<ApiResult<B>>()
    val job = launch { outcome.complete(call().toResult(transform)) }
    job.invokeOnCompletion { cause -> cause?.let { outcome.complete(ApiResult(null, CANCELLED)) } }
    return StreamJs(outcome.asPromise()) { job.cancel() }
}

/** A listener is the UI's code: its failure is reported, but must not break the stream that feeds it. */
internal fun callSafely(callback: () -> Unit) {
    Either.catch(callback).onLeft { console.error("hubmi-client: listener failed", it) }
}
