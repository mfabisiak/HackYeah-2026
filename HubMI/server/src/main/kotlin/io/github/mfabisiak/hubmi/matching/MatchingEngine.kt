package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.right
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError

interface MatchingEngine {
    /** [query] is the description with personal data already removed. */
    suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult>

    /** Prepares what the first [match] would otherwise build (indexes, vectors), so no user waits for it. */
    suspend fun warmUp(): Either<DomainError, Unit> = Unit.right()
}
