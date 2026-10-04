package io.github.mfabisiak.hubmi.web.adaptations

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationEvent
import io.github.mfabisiak.hubmi.api.AdaptationPlanDto
import io.github.mfabisiak.hubmi.api.AdaptationResponse
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.AdaptationStepDto
import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.InstitutionType
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.StreamJs
import io.github.mfabisiak.hubmi.web.mock.DEMO_MODEL_NAME
import io.github.mfabisiak.hubmi.web.mock.DEMO_USER_ID
import io.github.mfabisiak.hubmi.web.mock.MockAdaptation
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.estimatePilotCostPln
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlinx.coroutines.delay
import kotlin.js.Promise

/** The gap between the steps of a plan, as if a model were writing them. */
private const val STEP_DELAY_MS = 800L

/** A plan assembled from templates and the institution's numbers, streamed step by step like the real model's. */
internal class MockAdaptationsApi(
    private val backend: MockBackend,
) : AdaptationsApi {
    private val store = backend.store

    override fun request(
        innovationId: String,
        institution: InstitutionJs,
        listener: AdaptationListenerJs?,
    ): StreamJs<AdaptationResponseJs> =
        backend.stream(
            call = {
                val profile = institution.toDto().bind()
                ensure(profile.context.isNotBlank()) { invalid(blank("context")) }
                val innovation =
                    ensureNotNull(
                        store.db.innovations.firstOrNull { it.id == innovationId },
                    ) { notFound("innowacja $innovationId") }
                val steps = stepsFor(innovation, profile)
                steps.forEach { step ->
                    delay(STEP_DELAY_MS)
                    listener?.deliver(AdaptationEvent.Step(step))
                }
                val adaptation = adaptationOf(innovation, profile, steps)
                store.update { it.copy(adaptations = it.adaptations + MockAdaptation(adaptation, DEMO_USER_ID)) }
                AdaptationResponse(AiStatus.OK, adaptation)
            },
            transform = AdaptationResponse::toJs,
        )

    override fun mine(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdaptationJs>>> =
        backend.respond {
            store.db.adaptations
                .filter { it.authorId == DEMO_USER_ID }
                .map { it.adaptation }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun get(id: String): Promise<ApiResult<AdaptationJs>> =
        backend.respond {
            ensureNotNull(
                store.db.adaptations.firstOrNull { it.adaptation.id == id },
            ) { notFound("plan $id") }.adaptation.toJs()
        }

    private fun stepsFor(
        innovation: InnovationDto,
        institution: InstitutionProfile,
    ): List<AdaptationStepDto> =
        listOf(
            AdaptationStepDto(
                "Diagnoza lokalnych potrzeb",
                "Rozmowy z mieszkańcami i partnerami, wybór pierwszej lokalizacji dla „${innovation.title}”.",
                0,
            ),
            AdaptationStepDto(
                "Zespół i szkolenie",
                "Wyznaczenie ${institution.staffCount} osób do usługi i szkolenie z zasad działania rozwiązania.",
                1,
            ),
            AdaptationStepDto(
                "Start pilotażu",
                "Uruchomienie usługi w jednej lokalizacji i zbieranie opinii pierwszych uczestników.",
                2,
            ),
            AdaptationStepDto(
                "Monitoring i korekty",
                "Cotygodniowy przegląd wskaźników i poprawki dopasowane do warunków: " +
                    "${institution.context.take(CONTEXT_EXCERPT)}.",
                4,
            ),
            AdaptationStepDto(
                "Ewaluacja i decyzja o skalowaniu",
                "Podsumowanie wyników pilotażu i decyzja, czy objąć usługą kolejne miejsca.",
                6,
            ),
        )

    private fun adaptationOf(
        innovation: InnovationDto,
        institution: InstitutionProfile,
        steps: List<AdaptationStepDto>,
    ): AdaptationDto {
        val cost = estimatePilotCostPln(institution.staffCount)
        return AdaptationDto(
            id = newId("plan"),
            innovationId = innovation.id,
            innovationTitle = innovation.title,
            institution = institution,
            plan =
                AdaptationPlanDto(
                    serviceForm = "Usługa prowadzona przez ${typeLabel(
                        institution.type,
                    )} z udziałem ${institution.staffCount} osób i wolontariuszy.",
                    steps = steps,
                    requiredResources =
                        listOf(
                            "Lokal na spotkania",
                            "Koordynator na część etatu",
                            "Wolontariusze",
                            "Materiały informacyjne",
                        ),
                    risks =
                        listOf(
                            "Zbyt mała liczba chętnych do pomocy",
                            "Niska znajomość usługi wśród mieszkańców",
                            "Koszty po zakończeniu pilotażu",
                        ),
                    estimatedCostPln = cost,
                    exceedsBudget = cost > institution.budgetPln,
                ),
            model = DEMO_MODEL_NAME,
            status = AdaptationStatus.PENDING_REVIEW,
            adminComment = null,
            createdAt = nowIso(),
        )
    }

    private fun typeLabel(type: InstitutionType): String =
        when (type) {
            InstitutionType.LOCAL_GOVERNMENT -> "gminę"
            InstitutionType.NGO -> "organizację"
            InstitutionType.SOCIAL_SERVICES_CENTER -> "centrum usług społecznych"
            InstitutionType.OTHER -> "instytucję"
        }

    private companion object {
        const val CONTEXT_EXCERPT = 80
    }
}
