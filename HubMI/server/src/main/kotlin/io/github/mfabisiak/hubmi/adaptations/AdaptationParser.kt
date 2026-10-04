package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.assistant.ModelOutput
import io.github.mfabisiak.hubmi.assistant.ModelOutputError
import kotlinx.serialization.Serializable

/**
 * The model's plan is data to check, not to trust. A step that is blank, too long or outside the pilot is dropped, the
 * rest are put in the order they start, and the cost is compared with the budget instead of being believed: a plan that
 * costs more than the institution can spend is kept, but marked, so that an admin sees it.
 */
object AdaptationParser {
    const val MAX_TITLE_LENGTH = 100
    const val MAX_TEXT_LENGTH = 400
    const val MAX_ITEM_LENGTH = 200
    const val MIN_STEPS = 3
    const val MAX_STEPS = 8
    const val MAX_RESOURCES = 8
    const val MAX_RISKS = 6
    const val MAX_COST_PLN = 100_000_000

    @Serializable
    private data class RawStep(
        val title: String,
        val description: String,
        val startMonth: Int,
    )

    @Serializable
    private data class RawPlan(
        val serviceForm: String,
        val steps: List<RawStep>,
        val requiredResources: List<String>,
        val risks: List<String>,
        val estimatedCostPln: Int,
    )

    /** A single step, as soon as the model has written it. */
    fun step(raw: String): Either<ModelOutputError, AdaptationStep> =
        ModelOutput.decode<RawStep>(raw).flatMap(::validated)

    fun plan(
        raw: String,
        budgetPln: Int,
    ): Either<ModelOutputError, AdaptationPlan> =
        either {
            val parsed = ModelOutput.decode<RawPlan>(raw).bind()
            val steps = parsed.steps.mapNotNull { validated(it).getOrNull() }.sortedBy(AdaptationStep::startMonth)
            val resources = items(parsed.requiredResources)
            val risks = items(parsed.risks)
            ensure(steps.size in MIN_STEPS..MAX_STEPS) {
                ModelOutputError.Unusable("Liczba poprawnych kroków: ${steps.size}")
            }
            ensure(resources.isNotEmpty() && risks.isNotEmpty()) {
                ModelOutputError.Unusable("Brak zasobów lub ryzyk")
            }
            ensure(parsed.estimatedCostPln in 0..MAX_COST_PLN) {
                ModelOutputError.Unusable("Koszt poza zakresem: ${parsed.estimatedCostPln}")
            }
            AdaptationPlan(
                serviceForm = ModelOutput.text("serviceForm", parsed.serviceForm, MAX_TEXT_LENGTH).bind(),
                steps = steps,
                requiredResources = resources.take(MAX_RESOURCES),
                risks = risks.take(MAX_RISKS),
                estimatedCostPln = parsed.estimatedCostPln,
                exceedsBudget = parsed.estimatedCostPln > budgetPln,
            )
        }

    private fun validated(raw: RawStep): Either<ModelOutputError, AdaptationStep> =
        either {
            ensure(raw.startMonth in 0..AdaptationPrompts.LAST_MONTH) {
                ModelOutputError.Unusable("Miesiąc kroku poza pilotażem: ${raw.startMonth}")
            }
            AdaptationStep(
                title = ModelOutput.text("title", raw.title, MAX_TITLE_LENGTH).bind(),
                description = ModelOutput.text("description", raw.description, MAX_TEXT_LENGTH).bind(),
                startMonth = raw.startMonth,
            )
        }

    private fun items(raw: List<String>): List<String> =
        raw.map(String::trim).filter { it.isNotEmpty() && it.length <= MAX_ITEM_LENGTH }
}
