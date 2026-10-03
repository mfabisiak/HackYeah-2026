package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest

/** An [UpsertInnovationRequest] whose every field has been validated and normalised. */
data class InnovationDraft(
    val title: Title,
    val summary: Summary,
    val description: Description,
    val areas: NonEmptyList<SocialArea>,
    val targetGroups: NonEmptyList<TargetGroup>,
    val stage: InnovationStage,
    val region: Region?,
    val mediaUrls: List<HttpUrl>,
    val narrative: InnovationNarrative,
) {
    companion object {
        fun parse(request: UpsertInnovationRequest): EitherNel<FieldError, InnovationDraft> =
            Either.zipOrAccumulate(
                Title.parse(request.title).asNel(),
                Summary.parse(request.summary).asNel(),
                Description.parse(request.description).asNel(),
                request.areas.toNonEmpty("areas", "Wymagany jest co najmniej jeden obszar społeczny").asNel(),
                request.targetGroups
                    .toNonEmpty("targetGroups", "Wymagana jest co najmniej jedna grupa docelowa")
                    .asNel(),
                request.region.parseOptional(Region::parse).asNel(),
                request.mediaUrls.parseAll("mediaUrls", HttpUrl::parse),
                InnovationNarrative.parse(request),
            ) { title, summary, description, areas, targetGroups, region, mediaUrls, narrative ->
                InnovationDraft(
                    title = title,
                    summary = summary,
                    description = description,
                    areas = areas,
                    targetGroups = targetGroups,
                    stage = request.stage,
                    region = region,
                    mediaUrls = mediaUrls,
                    narrative = narrative,
                )
            }
    }
}
