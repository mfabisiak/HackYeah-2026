package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.DomainError

interface MatchingEngine {
    /** [query] is the description with personal data already removed. */
    suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult>
}
