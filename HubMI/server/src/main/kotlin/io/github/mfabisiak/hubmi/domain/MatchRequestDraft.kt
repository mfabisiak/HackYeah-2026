package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.MatchRequest

/** A [MatchRequest] whose every field has been validated and normalised. */
data class MatchRequestDraft(
    val description: NeedDescription,
    val municipality: MunicipalityName?,
) {
    companion object {
        fun parse(request: MatchRequest): EitherNel<FieldError, MatchRequestDraft> =
            Either.zipOrAccumulate(
                NeedDescription.parse(request.description).asNel(),
                request.municipality.parseOptional { MunicipalityName.parse(it, "municipality") }.asNel(),
            ) { description, municipality -> MatchRequestDraft(description, municipality) }
    }
}
