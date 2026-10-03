package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.MatchRequest
import io.github.mfabisiak.hubmi.challenges.MunicipalityName
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.parseOptional

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
