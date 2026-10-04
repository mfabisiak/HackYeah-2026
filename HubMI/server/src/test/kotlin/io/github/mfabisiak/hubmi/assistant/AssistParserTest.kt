package io.github.mfabisiak.hubmi.assistant

import io.github.mfabisiak.hubmi.matching.HybridTestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AssistParserTest {
    private val candidates =
        listOf(
            HybridTestFixtures.transport,
            HybridTestFixtures.loneliness,
        )

    private fun suggestion(
        title: String = "Dowóz do przychodni",
        rationale: String = "Seniorzy nie mają czym dojechać.",
        nextStep: String = "Znaleźć kierowców.",
        basedOn: String = "[1]",
    ) = """{"title":"$title","rationale":"$rationale","nextStep":"$nextStep","basedOn":$basedOn}"""

    @Test
    fun aGoodSuggestionIsReadAndItsNumbersBecomeInnovationIds() {
        val dto = AssistParser.suggestion(suggestion(basedOn = "[2, 1, 2]"), candidates).getOrNull()

        assertEquals("Dowóz do przychodni", dto?.title)
        assertEquals(
            listOf(HybridTestFixtures.loneliness.id.toHexString(), HybridTestFixtures.transport.id.toHexString()),
            dto?.basedOnInnovationIds,
        )
    }

    @Test
    fun anInventedNumberIsDroppedInsteadOfBecomingAnId() {
        val dto = AssistParser.suggestion(suggestion(basedOn = "[0, 3, 99, -1]"), candidates).getOrNull()

        assertEquals(emptyList(), dto?.basedOnInnovationIds)
    }

    @Test
    fun aSuggestionWithoutBasedOnIsStillValid() {
        val raw = """{"title":"A","rationale":"B","nextStep":"C"}"""

        assertEquals(emptyList(), AssistParser.suggestion(raw, candidates).getOrNull()?.basedOnInnovationIds)
    }

    @Test
    fun blankAndTooLongFieldsAreRejected() {
        val tooLong = "x".repeat(AssistParser.MAX_TEXT_LENGTH + 1)

        assertEquals(
            ModelOutputError.BlankField("title"),
            AssistParser.suggestion(suggestion(title = "  "), candidates).leftOrNull(),
        )
        assertEquals(
            ModelOutputError.TooLong("nextStep", AssistParser.MAX_TEXT_LENGTH),
            AssistParser.suggestion(suggestion(nextStep = tooLong), candidates).leftOrNull(),
        )
    }

    @Test
    fun somethingThatIsNotASuggestionIsMalformed() {
        assertIs<ModelOutputError.Malformed>(AssistParser.suggestion("""{"title":"A"}""", candidates).leftOrNull())
        assertIs<ModelOutputError.Malformed>(AssistParser.suggestion("not json", candidates).leftOrNull())
    }

    private fun flow(
        actors: String,
        steps: String,
    ) = """{"actors":$actors,"steps":$steps}"""

    private fun step(
        from: String,
        to: String,
        label: String = "robi coś",
    ) = """{"from":"$from","to":"$to","label":"$label"}"""

    @Test
    fun aFlowKeepsStepsBetweenKnownActorsAndNamesThemAsListed() {
        val raw =
            flow(
                """["Senior","Wolontariusz","Koordynator"]""",
                "[${step("senior", "KOORDYNATOR")},${step("koordynator", "Wolontariusz")}]",
            )

        val flow = AssistParser.flow(raw).getOrNull()

        assertEquals(listOf("Senior", "Wolontariusz", "Koordynator"), flow?.actors)
        val ends = flow?.steps?.map { step -> step.from to step.to }
        assertEquals(listOf("Senior" to "Koordynator", "Koordynator" to "Wolontariusz"), ends)
    }

    @Test
    fun aStepToAnActorNobodyListedIsDroppedAndTheRestSurvives() {
        val raw =
            flow(
                """["Młodzież","Psycholog"]""",
                "[${step("Brak", "Spotkanie")},${step("Młodzież", "Psycholog")},${step("Psycholog", "Młodzież")}]",
            )

        val flow = AssistParser.flow(raw).getOrNull()

        assertEquals(2, flow?.steps?.size)
    }

    @Test
    fun aFlowWithTooFewActorsOrUsableStepsIsUnusable() {
        val oneActor = flow("""["Senior"]""", "[${step("Senior", "Senior")},${step("Senior", "Senior")}]")
        val oneUsableStep = flow("""["A","B"]""", "[${step("A", "B")},${step("X", "Y")}]")

        assertIs<ModelOutputError.Unusable>(AssistParser.flow(oneActor).leftOrNull())
        assertIs<ModelOutputError.Unusable>(AssistParser.flow(oneUsableStep).leftOrNull())
    }
}
