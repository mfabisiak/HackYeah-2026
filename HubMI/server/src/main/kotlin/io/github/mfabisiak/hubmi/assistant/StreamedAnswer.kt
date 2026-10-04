package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either

/** What has come out of the model so far: its text, the array elements it finished with the last piece, how it ended. */
internal data class StreamedAnswer(
    val text: String,
    val finished: List<String>,
    val failure: LlmError?,
    private val scanner: JsonArrayElements,
) {
    fun after(piece: Either<LlmError, String>): StreamedAnswer =
        piece.fold(
            ifLeft = { copy(finished = emptyList(), failure = it) },
            ifRight = { chunk ->
                val fed = scanner.feed(chunk)
                copy(text = text + chunk, finished = fed.fresh, scanner = fed.scanner)
            },
        )

    companion object {
        val START = StreamedAnswer(text = "", finished = emptyList(), failure = null, scanner = JsonArrayElements.EMPTY)
    }
}
