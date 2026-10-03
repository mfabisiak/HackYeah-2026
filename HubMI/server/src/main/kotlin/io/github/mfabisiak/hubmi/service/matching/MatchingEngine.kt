package io.github.mfabisiak.hubmi.service.matching

import arrow.core.Either
import io.github.mfabisiak.hubmi.domain.MunicipalityName
import io.github.mfabisiak.hubmi.service.DomainError

interface MatchingEngine {
    /** [query] is the description with personal data already removed. */
    suspend fun match(
        query: String,
        municipality: MunicipalityName?,
    ): Either<DomainError, EngineResult>
}
