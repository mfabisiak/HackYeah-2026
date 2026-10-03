package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Uses [primary] and, when its backing service is unavailable, [fallback]. After a failure the primary is left alone
 * for [cooldown], so a stopped Ollama costs one failed call instead of a timeout on every request.
 */
class FallbackEngine(
    private val primary: MatchingEngine,
    private val fallback: MatchingEngine,
    private val clock: Clock = Clock.systemUTC(),
    private val cooldown: Duration = DEFAULT_COOLDOWN,
) : MatchingEngine {
    private val retryAt = AtomicReference<Instant?>(null)

    override suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult> =
        if (cooling()) {
            fallback.match(query, municipality)
        } else {
            primary.match(query, municipality).fold(
                ifLeft = { error -> if (degrade(error)) fallback.match(query, municipality) else error.left() },
                ifRight = { it.right() },
            )
        }

    override suspend fun warmUp(): Either<DomainError, Unit> = primary.warmUp().onLeft { degrade(it) }

    /** Starts the cooldown when [error] says the service is down; other errors are not something a fallback fixes. */
    private fun degrade(error: DomainError): Boolean =
        (error is DomainError.Unavailable).also { unavailable ->
            if (unavailable) {
                retryAt.set(clock.instant().plus(cooldown))
                log.warn("Silnik główny niedostępny, używam wyszukiwania słownego przez {}", cooldown)
            }
        }

    private fun cooling(): Boolean = retryAt.get()?.isAfter(clock.instant()) == true

    private companion object {
        val DEFAULT_COOLDOWN: Duration = Duration.ofSeconds(30)
        val log = LoggerFactory.getLogger(FallbackEngine::class.java)
    }
}
