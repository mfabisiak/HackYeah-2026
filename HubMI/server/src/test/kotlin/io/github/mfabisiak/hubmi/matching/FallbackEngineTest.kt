package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.Catalogue
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.garden
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.loneliness
import io.github.mfabisiak.hubmi.matching.HybridTestFixtures.transport
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FallbackEngineTest {
    private class MovableClock(
        private val now: AtomicReference<Instant> = AtomicReference(Instant.parse("2026-10-01T10:00:00Z")),
    ) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant(): Instant = now.get()

        fun advance(duration: Duration) = now.updateAndGet { it.plus(duration) }
    }

    private val clock = MovableClock()
    private val engines = HybridTestFixtures.engines(Catalogue(listOf(transport, garden, loneliness)))
    private val engine =
        FallbackEngine(
            primary = engines.hybrid,
            fallback = engines.keyword,
            clock = clock,
            cooldown = Duration.ofSeconds(30),
        )
    private val semanticQuery = "Brak jak dojechać do lekarza"

    private fun match(query: String) = runBlocking { engine.match(query, null) }

    @Test
    fun usesThePrimaryWhileItWorks() {
        val result = assertIs<Either.Right<EngineResult>>(match(semanticQuery)).value

        assertEquals(
            transport.id,
            result.matches
                .first()
                .innovation.id,
        )
    }

    @Test
    fun fallsBackToKeywordsWhenTheEmbedderIsDownInsteadOfFailing() {
        engines.embedder.down.set(true)

        val result = assertIs<Either.Right<EngineResult>>(match("samotnych seniorów potrzebują rozmowy")).value

        assertEquals(
            loneliness.id,
            result.matches
                .first()
                .innovation.id,
        )
    }

    @Test
    fun doesNotAskTheBrokenPrimaryAgainUntilTheCooldownPasses() {
        engines.embedder.down.set(true)
        match("rozmowy")
        val callsAfterFailure = engines.embedder.calls.get()

        match("rozmowy")
        match("rozmowy")

        assertEquals(callsAfterFailure, engines.embedder.calls.get(), "no calls while cooling down")
    }

    @Test
    fun returnsToThePrimaryAfterTheCooldown() {
        engines.embedder.down.set(true)
        match(semanticQuery)
        engines.embedder.down.set(false)
        assertTrue(assertIs<Either.Right<EngineResult>>(match(semanticQuery)).value.noGoodMatch, "still on keywords")

        clock.advance(Duration.ofSeconds(31))

        assertEquals(
            transport.id,
            assertIs<Either.Right<EngineResult>>(match(semanticQuery))
                .value.matches
                .first()
                .innovation.id,
        )
    }

    @Test
    fun errorsOtherThanUnavailableAreNotHiddenByTheFallback() {
        val broken =
            object : MatchingEngine {
                override suspend fun match(
                    query: String,
                    municipality: io.github.mfabisiak.hubmi.challenges.MunicipalityName?,
                ) = Either.Left(DomainError.Internal())
            }

        val result = runBlocking { FallbackEngine(broken, engines.keyword, clock).match("rozmowy", null) }

        assertIs<DomainError.Internal>(result.leftOrNull())
    }

    @Test
    fun aFailedWarmUpStartsTheCooldownSoTheFirstUserIsNotTheOneWhoWaits() {
        engines.embedder.down.set(true)
        assertIs<DomainError.Unavailable>(runBlocking { engine.warmUp() }.leftOrNull())
        val calls = engines.embedder.calls.get()

        match("rozmowy")

        assertEquals(calls, engines.embedder.calls.get())
    }
}
