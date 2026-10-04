package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.matching.EngineResult
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures
import io.github.mfabisiak.hubmi.matching.MatchingEngine
import io.github.mfabisiak.hubmi.matching.ScoredInnovation
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicInteger

/** Answers the way the model does: the text arrives in small pieces, and may end in a failure or never end. */
class FakeLlmClient(
    private val pieces: List<Either<LlmError, String>>,
    private val hangsAfterwards: Boolean = false,
    private val latencyMillis: Long = 0,
) : LlmClient {
    val requests = AtomicInteger(0)
    val running = AtomicInteger(0)
    val mostRunningAtOnce = AtomicInteger(0)

    override fun stream(request: LlmRequest): Flow<Either<LlmError, String>> =
        flow {
            requests.incrementAndGet()
            mostRunningAtOnce.accumulateAndGet(running.incrementAndGet(), Math::max)
            delay(latencyMillis)
            pieces.forEach { emit(it) }
            if (hangsAfterwards) awaitCancellation()
        }.onCompletion { running.decrementAndGet() }

    companion object {
        fun answering(
            text: String,
            pieceLength: Int = 9,
            latencyMillis: Long = 0,
        ) = FakeLlmClient(text.chunked(pieceLength).map { it.right() }, latencyMillis = latencyMillis)

        fun failing(error: LlmError) = FakeLlmClient(listOf(error.left()))
    }
}

/** Finds a fixed set of innovations whatever the idea says. */
class FakeMatchingEngine(
    private val matches: List<ScoredInnovation>,
) : MatchingEngine {
    override suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult> = EngineResult(matches).right()
}

object AssistFixtures {
    val transport = HybridTestFixtures.transport
    val loneliness = HybridTestFixtures.loneliness

    fun scored(
        score: Double,
        vararg innovations: InnovationItem,
    ) = innovations.map { ScoredInnovation(it, score, matchedTerms = emptyList(), reasons = emptyList()) }

    /** An answer of Bielik 4.5B to an idea of a youth club, recorded and edited: its first suggestion cites an invented 7. */
    val suggestionsAnswer =
        listOf(
            """{"suggestions":[""",
            """{"title":"Klub Rozmów i Relaksu",""",
            """"rationale":"Odpowiada na potrzebę wsparcia emocjonalnego młodzieży po pandemii.",""",
            """"nextStep":"Znaleźć lokalnego psychologa do prowadzenia spotkań.","basedOn":[1,7]},""",
            """{"title":"Wieczorne Spotkania z Planszówkami","rationale":"Zapewnia młodzieży przestrzeń do integracji.",""",
            """"nextStep":"Przygotować harmonogram spotkań.","basedOn":[]},""",
            """{"title":"Klub Rozmów i Warsztatów","rationale":"Dodaje elementy edukacyjne.",""",
            """"nextStep":"Zorganizować warsztaty z rozwiązywania konfliktów.","basedOn":[2]}]}""",
        ).joinToString("")

    /** An answer of Bielik 4.5B for the idea of a ride service for seniors, recorded as it was. */
    val flowAnswer =
        listOf(
            """{"actors":["senior","wolontariusz","linia telefoniczna"],"steps":[""",
            """{"from":"senior","to":"linia telefoniczna","label":"zamawia przejazd"},""",
            """{"from":"linia telefoniczna","to":"wolontariusz","label":"przekazuje zamówienie"},""",
            """{"from":"wolontariusz","to":"senior","label":"odbiera i dowozi do lekarza"}]}""",
        ).joinToString("")
}
