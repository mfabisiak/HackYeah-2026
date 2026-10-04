package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.assistant.ModelOutputError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdaptationParserTest {
    private fun step(
        title: String = "Start pilotażu",
        description: String = "Pierwsze przejazdy.",
        startMonth: Int = 3,
    ) = """{"title":"$title","description":"$description","startMonth":$startMonth}"""

    private fun plan(
        steps: List<String> = listOf(step(startMonth = 0), step(startMonth = 1), step(startMonth = 2)),
        resources: String = """["Koordynator"]""",
        risks: String = """["Za mało kierowców"]""",
        cost: Int = 45_000,
    ) = """{"serviceForm":"Usługa gminna","steps":[${steps.joinToString(",")}],""" +
        """"requiredResources":$resources,"risks":$risks,"estimatedCostPln":$cost}"""

    @Test
    fun aGoodPlanIsReadAsItWasWritten() {
        val parsed = AdaptationParser.plan(AdaptationFixtures.planAnswer(), budgetPln = 60_000).getOrNull()

        assertEquals(4, parsed?.steps?.size)
        assertEquals(listOf(0, 1, 2, 3), parsed?.steps?.map(AdaptationStep::startMonth))
        assertEquals(45_000, parsed?.estimatedCostPln)
        assertEquals(false, parsed?.exceedsBudget)
    }

    @Test
    fun aCostAboveTheBudgetIsKeptAndMarked() {
        val parsed =
            AdaptationParser
                .plan(
                    AdaptationFixtures.planAnswer(costPln = 80_000),
                    budgetPln = 60_000,
                ).getOrNull()

        assertEquals(80_000, parsed?.estimatedCostPln)
        assertEquals(true, parsed?.exceedsBudget)
    }

    @Test
    fun aCostEqualToTheBudgetFits() {
        assertEquals(false, AdaptationParser.plan(plan(cost = 60_000), budgetPln = 60_000).getOrNull()?.exceedsBudget)
    }

    @Test
    fun stepsAreOrderedByTheMonthTheyStartIn() {
        val raw = plan(listOf(step("C", startMonth = 5), step("A", startMonth = 0), step("B", startMonth = 2)))

        val titles =
            AdaptationParser
                .plan(raw, budgetPln = 1)
                .getOrNull()
                ?.steps
                ?.map(AdaptationStep::title)

        assertEquals(listOf("A", "B", "C"), titles)
    }

    @Test
    fun aStepOutsideThePilotOrWithoutATitleIsDroppedAndTheRestSurvives() {
        val raw =
            plan(
                listOf(
                    step("A", startMonth = 0),
                    step("B", startMonth = 12),
                    step("  ", startMonth = 1),
                    step("C", startMonth = 2),
                    step("D", startMonth = -1),
                    step("E", startMonth = 3),
                ),
            )

        val titles =
            AdaptationParser
                .plan(raw, budgetPln = 1)
                .getOrNull()
                ?.steps
                ?.map(AdaptationStep::title)

        assertEquals(listOf("A", "C", "E"), titles)
    }

    @Test
    fun tooFewUsableStepsMakeThePlanUnusable() {
        val raw = plan(listOf(step(startMonth = 0), step(startMonth = 1), step(startMonth = 99)))

        assertIs<ModelOutputError.Unusable>(AdaptationParser.plan(raw, budgetPln = 1).leftOrNull())
    }

    @Test
    fun aPlanWithoutResourcesOrRisksOrWithAWildCostIsUnusable() {
        assertIs<ModelOutputError.Unusable>(AdaptationParser.plan(plan(resources = "[]"), 1).leftOrNull())
        assertIs<ModelOutputError.Unusable>(AdaptationParser.plan(plan(risks = """[" "]"""), 1).leftOrNull())
        assertIs<ModelOutputError.Unusable>(AdaptationParser.plan(plan(cost = -5), 1).leftOrNull())
        assertIs<ModelOutputError.Unusable>(
            AdaptationParser.plan(plan(cost = AdaptationParser.MAX_COST_PLN + 1), 1).leftOrNull(),
        )
    }

    @Test
    fun somethingThatIsNotAPlanIsMalformed() {
        assertIs<ModelOutputError.Malformed>(AdaptationParser.plan("""{"serviceForm":"x"}""", 1).leftOrNull())
        assertIs<ModelOutputError.Malformed>(AdaptationParser.plan("nie json", 1).leftOrNull())
    }

    @Test
    fun aSingleStepIsCheckedTheSameWay() {
        assertEquals(AdaptationStep("Start", "Opis", 2), AdaptationParser.step(step("Start", "Opis", 2)).getOrNull())
        assertIs<ModelOutputError.Unusable>(AdaptationParser.step(step(startMonth = 40)).leftOrNull())
        assertIs<ModelOutputError.BlankField>(AdaptationParser.step(step(description = "")).leftOrNull())
    }
}
