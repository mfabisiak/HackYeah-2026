package io.github.mfabisiak.hubmi.api

import kotlin.test.Test
import kotlin.test.assertEquals

class SseParserTest {
    private fun parse(stream: String): List<SseMessage> =
        stream
            .split("\n")
            .runningFold(SseParser.Step(SseParser.INITIAL, null)) { step, line -> step.parser.accept(line) }
            .mapNotNull(SseParser.Step::message)

    @Test
    fun anEventIsDispatchedByTheBlankLineAfterIt() {
        assertEquals(
            listOf(SseMessage("similar", """{"a":1}""")),
            parse("event: similar\ndata: {\"a\":1}\n\n"),
        )
    }

    @Test
    fun severalEventsComeOutInOrder() {
        val messages = parse("event: suggestion\ndata: 1\n\nevent: done\ndata: 2\n\n")

        assertEquals(listOf("suggestion", "done"), messages.map(SseMessage::event))
        assertEquals(listOf("1", "2"), messages.map(SseMessage::data))
    }

    @Test
    fun dataLinesAreJoinedWithNewlines() {
        assertEquals(listOf(SseMessage(null, "a\nb")), parse("data: a\ndata: b\n\n"))
    }

    @Test
    fun onlyOneSpaceAfterTheColonIsPartOfTheSeparator() {
        assertEquals(listOf(SseMessage(null, " x")), parse("data:  x\n\n"))
        assertEquals(listOf(SseMessage(null, "x")), parse("data:x\n\n"))
    }

    @Test
    fun commentsAndUnknownFieldsAreSkipped() {
        assertEquals(
            listOf(SseMessage("done", "x")),
            parse(": keep-alive\nretry: 15000\nid: 7\nevent: done\ndata: x\n\n"),
        )
    }

    @Test
    fun blankLinesWithoutDataDispatchNothing() {
        assertEquals(emptyList(), parse("\n\nevent: x\n\n"))
    }

    @Test
    fun anUnfinishedEventAtTheEndOfTheStreamIsDropped() {
        assertEquals(emptyList(), parse("event: done\ndata: x"))
    }

    @Test
    fun theEventNameDoesNotLeakIntoTheNextEvent() {
        assertEquals(listOf(SseMessage("a", "1"), SseMessage(null, "2")), parse("event: a\ndata: 1\n\ndata: 2\n\n"))
    }
}
