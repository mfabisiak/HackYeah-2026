package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest

/** An [UpsertChallengeRequest] whose every field has been validated and normalised. */
data class ChallengeDraft(
    val title: Title,
    val description: Description,
    val area: SocialArea,
    val municipalities: List<MunicipalityName>,
) {
    companion object {
        fun parse(request: UpsertChallengeRequest): EitherNel<FieldError, ChallengeDraft> =
            Either.zipOrAccumulate(
                Title.parse(request.title).asNel(),
                Description.parse(request.description).asNel(),
                request.municipalities.parseAll("municipalities", MunicipalityName::parse),
            ) { title, description, municipalities ->
                ChallengeDraft(
                    title = title,
                    description = description,
                    area = request.area,
                    municipalities = municipalities.distinct(),
                )
            }
    }
}
