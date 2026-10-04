package io.github.mfabisiak.hubmi.web.assistant

import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.AssistEvent
import io.github.mfabisiak.hubmi.api.AssistMode
import io.github.mfabisiak.hubmi.api.AssistResponse
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.FlowDto
import io.github.mfabisiak.hubmi.api.FlowStepDto
import io.github.mfabisiak.hubmi.api.NoveltyHint
import io.github.mfabisiak.hubmi.api.SimilarInnovationDto
import io.github.mfabisiak.hubmi.api.SuggestionDto
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.web.StreamJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.ideas.CreateIdeaJs
import io.github.mfabisiak.hubmi.web.ideas.toDto
import io.github.mfabisiak.hubmi.web.mock.DemoSeed
import io.github.mfabisiak.hubmi.web.mock.MatchHit
import io.github.mfabisiak.hubmi.web.mock.MatchIndex
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.toSummary
import kotlinx.coroutines.delay

/** The gap between the parts of an answer, as if a model were writing them. */
private const val STEP_DELAY_MS = 700L
private const val ALREADY_EXISTS_RELEVANCE = 0.75
private const val SIMILAR_LIMIT = 3

/** An assistant that needs no model: the similar innovations are really computed, the suggestions are templates. */
internal class MockAssistantApi(
    private val backend: MockBackend,
) : AssistantApi {
    private val store = backend.store

    override fun assistDraft(
        idea: CreateIdeaJs,
        mode: String,
        listener: AssistListenerJs?,
    ): StreamJs<AssistResponseJs> =
        backend.stream(
            call = { answer(idea.toDto().bind(), enumOf<AssistMode>(mode, "mode"), listener) },
            transform = AssistResponse::toJs,
        )

    override fun assistIdea(
        ideaId: String,
        mode: String,
        listener: AssistListenerJs?,
    ): StreamJs<AssistResponseJs> =
        backend.stream(
            call = {
                val saved =
                    ensureNotNull(
                        store.db.ideas.firstOrNull { it.idea.id == ideaId },
                    ) { notFound("pomysł $ideaId") }.idea
                val idea = CreateIdeaRequest(saved.title, saved.essence, saved.targetGroups, saved.stage)
                answer(idea, enumOf<AssistMode>(mode, "mode"), listener)
            },
            transform = AssistResponse::toJs,
        )

    private suspend fun answer(
        idea: CreateIdeaRequest,
        mode: AssistMode,
        listener: AssistListenerJs?,
    ): AssistResponse {
        val hits =
            MatchIndex(
                store.db.innovations,
                DemoSeed.keywords,
            ).search(idea.title + " " + idea.essence, limit = SIMILAR_LIMIT)
        val similar = hits.map { SimilarInnovationDto(it.innovation.toSummary(), it.relevance) }
        val novelty = noveltyOf(hits.firstOrNull())
        listener?.deliver(AssistEvent.Similar(similar, novelty))
        val suggestions =
            when (mode) {
                AssistMode.EXPAND -> expandSuggestions(idea, hits)
                AssistMode.RISKS -> riskSuggestions(hits)
                AssistMode.SIMILAR, AssistMode.FLOW -> emptyList()
            }
        suggestions.forEach { suggestion ->
            delay(STEP_DELAY_MS)
            listener?.deliver(AssistEvent.Suggestion(suggestion))
        }
        val flow =
            if (mode == AssistMode.FLOW) {
                delay(STEP_DELAY_MS)
                flowOf(idea).also { listener?.deliver(AssistEvent.Flow(it)) }
            } else {
                null
            }
        return AssistResponse(
            mode = mode,
            aiStatus = if (mode == AssistMode.SIMILAR) AiStatus.NOT_REQUESTED else AiStatus.OK,
            similar = similar,
            noveltyHint = novelty,
            suggestions = suggestions,
            flow = flow,
        )
    }

    private fun noveltyOf(best: MatchHit?): NoveltyHint =
        when {
            best == null -> NoveltyHint.NEW
            best.relevance >= ALREADY_EXISTS_RELEVANCE -> NoveltyHint.ALREADY_EXISTS
            else -> NoveltyHint.PARTIAL
        }

    private fun expandSuggestions(
        idea: CreateIdeaRequest,
        hits: List<MatchHit>,
    ): List<SuggestionDto> {
        val example =
            hits
                .firstOrNull()
                ?.let {
                    " Podobna innowacja „${it.innovation.title}” również zaczynała od małej skali."
                }.orEmpty()
        return listOf(
            SuggestionDto(
                title = "Zacznij od jednej lokalizacji",
                rationale =
                    "Pomysł „${idea.title}” najłatwiej sprawdzić w jednej gminie " +
                        "lub dzielnicy, zanim trafi do wielu miejsc.$example",
                nextStep = "Wybierz partnera lokalnego i opisz pierwsze trzy miesiące pilotażu.",
                basedOnInnovationIds = hits.take(1).map { it.innovation.id },
            ),
            SuggestionDto(
                title = "Zaangażuj odbiorców w projektowanie",
                rationale = "Grupa docelowa (${idea.targetGroups.joinToString(
                    ", ",
                ) {
                    groupLabel(
                        it,
                    )
                }}) najlepiej wie, czego potrzebuje. Krótkie warsztaty pozwolą poprawić pomysł jeszcze przed startem.",
                nextStep = "Zorganizuj dwa spotkania konsultacyjne i zapisz wnioski.",
                basedOnInnovationIds = hits.drop(1).take(1).map { it.innovation.id },
            ),
            SuggestionDto(
                title = "Zaplanuj mierzenie efektów",
                rationale =
                    "Bez prostych wskaźników trudno pokazać, że pomysł działa, a " +
                        "to ważne przy wniosku o grant.",
                nextStep = "Ustal dwa lub trzy wskaźniki, na przykład liczbę uczestników i ich ocenę usługi.",
                basedOnInnovationIds = hits.map { it.innovation.id },
            ),
        )
    }

    private fun riskSuggestions(hits: List<MatchHit>): List<SuggestionDto> =
        listOf(
            SuggestionDto(
                title = "Zbyt mało zaangażowanych osób",
                rationale =
                    "Inicjatywy oparte na wolontariuszach często słabną po " +
                        "pierwszych miesiącach, gdy mija entuzjazm.",
                nextStep = "Zaplanuj rekrutację z zapasem i prosty system podziękowań dla wolontariuszy.",
                basedOnInnovationIds = hits.take(1).map { it.innovation.id },
            ),
            SuggestionDto(
                title = "Brak finansowania po pilotażu",
                rationale = "Grant pokrywa start, ale usługa potrzebuje źródła utrzymania także później.",
                nextStep = "Już w planie pilotażu wskaż, kto przejmie koszty po jego zakończeniu.",
                basedOnInnovationIds = emptyList(),
            ),
            SuggestionDto(
                title = "Ochrona danych uczestników",
                rationale =
                    "Przy pracy z osobami starszymi lub młodzieżą zbierasz dane " +
                        "wrażliwe, za które odpowiadasz.",
                nextStep = "Zbieraj tylko niezbędne dane i przygotuj krótką klauzulę informacyjną.",
                basedOnInnovationIds = hits.drop(1).map { it.innovation.id },
            ),
        )

    private fun flowOf(idea: CreateIdeaRequest): FlowDto {
        val participant = idea.targetGroups.firstOrNull()?.let(::groupLabel) ?: "Uczestnik"
        val coordinator = "Koordynator"
        val partner = "Partner lokalny"
        return FlowDto(
            actors = listOf(participant, coordinator, partner),
            steps =
                listOf(
                    FlowStepDto(participant, coordinator, "Zgłasza potrzebę"),
                    FlowStepDto(coordinator, partner, "Szuka osoby lub miejsca do pomocy"),
                    FlowStepDto(partner, participant, "Udziela wsparcia"),
                    FlowStepDto(participant, coordinator, "Ocenia pomoc"),
                    FlowStepDto(coordinator, partner, "Przekazuje uwagi i poprawia usługę"),
                ),
        )
    }

    private fun groupLabel(group: TargetGroup): String =
        when (group) {
            TargetGroup.SENIORS -> "Senior"
            TargetGroup.YOUTH -> "Młoda osoba"
            TargetGroup.PEOPLE_WITH_DISABILITIES -> "Osoba z niepełnosprawnością"
            TargetGroup.FAMILIES -> "Rodzina"
            TargetGroup.RESIDENTS -> "Mieszkaniec"
            TargetGroup.NGOS -> "Organizacja pozarządowa"
            TargetGroup.LOCAL_GOVERNMENTS -> "Samorząd"
        }
}
