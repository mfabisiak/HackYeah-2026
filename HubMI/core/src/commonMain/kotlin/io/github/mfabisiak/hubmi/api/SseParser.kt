package io.github.mfabisiak.hubmi.api

/** One dispatched server-sent event: its `event:` name (absent for the default one) and its `data:` lines joined. */
data class SseMessage(
    val event: String?,
    val data: String,
)

/**
 * Reads a `text/event-stream` line by line, without state to mutate: [accept] returns the parser to use for the next
 * line plus the message that line completed, if any. The browser's `EventSource` does the same, but cannot send the
 * `Authorization` header, so the web client reads the stream itself and needs the parsing.
 *
 * Only `event:` and `data:` matter here; comments (`:`), `id:` and `retry:` are skipped, as is an unfinished event at
 * the end of the stream, which the specification says to discard.
 */
class SseParser private constructor(
    private val event: String?,
    private val data: List<String>,
) {
    data class Step(
        val parser: SseParser,
        val message: SseMessage?,
    )

    fun accept(line: String): Step =
        when {
            line.isEmpty() -> Step(INITIAL, message())
            line.startsWith(COMMENT_PREFIX) -> Step(this, null)
            else -> Step(withField(line.substringBefore(FIELD_SEPARATOR), valueOf(line)), null)
        }

    private fun message(): SseMessage? =
        data.takeIf { it.isNotEmpty() }?.let { SseMessage(event, it.joinToString("\n")) }

    private fun valueOf(line: String): String = line.substringAfter(FIELD_SEPARATOR, "").removePrefix(" ")

    private fun withField(
        name: String,
        value: String,
    ): SseParser =
        when (name) {
            EVENT_FIELD -> SseParser(value.ifEmpty { null }, data)
            DATA_FIELD -> SseParser(event, data + value)
            else -> this
        }

    companion object {
        val INITIAL = SseParser(null, emptyList())

        private const val COMMENT_PREFIX = ":"
        private const val FIELD_SEPARATOR = ":"
        private const val EVENT_FIELD = "event"
        private const val DATA_FIELD = "data"
    }
}
