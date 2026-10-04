package io.github.mfabisiak.hubmi.assistant

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonArrayElementsTest {
    private val answer =
        """{"suggestions":[{"title":"A","basedOn":[1,2]},{"title":"B } ] \" {","nested":{"x":[{"y":1}]}}]}"""
    private val first = """{"title":"A","basedOn":[1,2]}"""
    private val second = """{"title":"B } ] \" {","nested":{"x":[{"y":1}]}}"""

    private fun scan(chunks: List<String>): List<List<String>> =
        chunks.runningFold(JsonArrayElements.EMPTY.feed("")) { fed, chunk -> fed.scanner.feed(chunk) }.map { it.fresh }

    @Test
    fun anElementIsReportedWhenItsClosingBraceArrives() {
        val fresh = scan(listOf("""{"suggestions":[{"title":"A","basedOn":[1,2]""", "}", """,{"title":"B"}]}"""))

        assertEquals(listOf(emptyList(), listOf(first), listOf("""{"title":"B"}""")), fresh.drop(1))
    }

    @Test
    fun bracesAndQuotesInsideStringsDoNotConfuseIt() {
        assertEquals(listOf(first, second), scan(listOf(answer)).flatten())
    }

    @Test
    fun theChunkingDoesNotChangeTheResult() {
        listOf(1, 2, 3, 7, 13, answer.length).forEach { size ->
            assertEquals(listOf(first, second), scan(answer.chunked(size)).flatten(), "chunk size $size")
        }
    }

    @Test
    fun anElementStillBeingWrittenIsNotReported() {
        assertEquals(emptyList(), scan(listOf("""{"suggestions":[{"title":"A","basedOn":[1,""")).flatten())
    }

    @Test
    fun nothingIsReportedBeforeTheArrayOpens() {
        assertEquals(emptyList(), scan(listOf("""{"other":{"a":1},"suggestions":[]}""")).flatten())
    }
}
