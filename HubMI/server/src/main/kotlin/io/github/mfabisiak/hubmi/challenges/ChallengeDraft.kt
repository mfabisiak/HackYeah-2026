package io.github.mfabisiak.hubmi.challenges

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.common.Description
import io.github.mfabisiak.hubmi.common.Title
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.parseAll

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
