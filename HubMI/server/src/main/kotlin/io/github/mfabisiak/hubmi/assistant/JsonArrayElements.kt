package io.github.mfabisiak.hubmi.assistant

/**
 * Picks the finished objects out of the array of a JSON answer that is still being written, as in
 * `{"suggestions":[{...},{...` where each `{...}` can be shown as soon as its closing brace arrives. It reads the single
 * array nested one level deep in the root object, which is how every schema of the assistant is shaped, and does not
 * check the JSON: a piece that fails to decode is dropped by whoever decodes it.
 */
internal data class JsonArrayElements(
    /** Brackets open at the moment: the root object (1), the array (2), an element object (3) and what is inside it. */
    private val depth: Int,
    private val inString: Boolean,
    private val escaped: Boolean,
    /** The element being written, from its opening brace. */
    private val current: String,
    val completed: List<String>,
) {
    /** The scanner after a chunk, and the elements that chunk finished. */
    data class Fed(
        val scanner: JsonArrayElements,
        val fresh: List<String>,
    )

    fun feed(chunk: String): Fed {
        val next = chunk.fold(this) { scanner, char -> scanner.step(char) }
        return Fed(scanner = next, fresh = next.completed.drop(completed.size))
    }

    private fun step(char: Char): JsonArrayElements =
        when {
            inString -> {
                copy(
                    inString = escaped || char != QUOTE,
                    escaped = !escaped && char == BACKSLASH,
                    current = collect(char),
                )
            }

            char == QUOTE -> {
                copy(inString = true, current = collect(char))
            }

            char == OBJECT_START || char == ARRAY_START -> {
                copy(depth = depth + 1, current = collect(char, depth + 1))
            }

            char == OBJECT_END && depth == ELEMENT_DEPTH -> {
                copy(
                    depth = depth - 1,
                    current = "",
                    completed =
                        completed + (current + char),
                )
            }

            char == OBJECT_END || char == ARRAY_END -> {
                copy(depth = depth - 1, current = collect(char))
            }

            else -> {
                copy(current = collect(char))
            }
        }

    /** Characters belong to an element once its object has opened; [atDepth] is the depth the character is read at. */
    private fun collect(
        char: Char,
        atDepth: Int = depth,
    ): String = if (atDepth >= ELEMENT_DEPTH) current + char else current

    companion object {
        val EMPTY =
            JsonArrayElements(depth = 0, inString = false, escaped = false, current = "", completed = emptyList())

        private const val ELEMENT_DEPTH = 3
        private const val QUOTE = '"'
        private const val BACKSLASH = '\\'
        private const val OBJECT_START = '{'
        private const val OBJECT_END = '}'
        private const val ARRAY_START = '['
        private const val ARRAY_END = ']'
    }
}
