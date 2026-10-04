package io.github.mfabisiak.hubmi.common.http

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val NO_BUFFERING_HEADER = "X-Accel-Buffering"
private const val NO_BUFFERING = "no"
private const val HEARTBEAT_LINE = ": keep-alive\n\n"

/** Often enough for Netty (it closes a response that is silent for 10 s) and for a reverse proxy in front of it. */
private val HEARTBEAT: Duration = 5.seconds

/** Whether the client asked for server-sent events instead of one JSON answer. */
fun ApplicationCall.wantsEventStream(): Boolean =
    request.header(HttpHeaders.Accept).orEmpty().contains(ContentType.Text.EventStream.toString())

/**
 * Sends [events] as server-sent events, each flushed at once so that the client sees it when it happens. [nameOf] is
 * the `event:` name; `data:` is the JSON of the event, which `serializer` (of a sealed type) tags with its `type`.
 *
 * A comment line goes out at once and then every few seconds, because the events can be far apart (a local model that
 * is busy with someone else, or loading) and an idle connection is closed by the server and by proxies.
 */
suspend fun <E : Any> RoutingContext.respondEventStream(
    events: Flow<E>,
    serializer: KSerializer<E>,
    nameOf: (E) -> String,
) {
    call.response.cacheControl(CacheControl.NoCache(null))
    call.response.header(NO_BUFFERING_HEADER, NO_BUFFERING)
    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
        withHeartbeat(events).collect { event ->
            if (event == null) {
                write(HEARTBEAT_LINE)
            } else {
                write("event: ${nameOf(event)}\n")
                write("data: ${DefaultJson.encodeToString(serializer, event)}\n\n")
            }
            flush()
        }
    }
}

/** The events, with a `null` at the start and then one every [HEARTBEAT] for as long as the events last. */
private fun <E : Any> withHeartbeat(events: Flow<E>): Flow<E?> =
    channelFlow {
        val ticker =
            launch {
                while (true) {
                    send(null)
                    delay(HEARTBEAT)
                }
            }
        events.collect { send(it) }
        ticker.cancel()
    }
