package io.github.mfabisiak.hubmi.materials

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.common.Description
import io.github.mfabisiak.hubmi.common.HttpUrl
import io.github.mfabisiak.hubmi.common.Title
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.toNonEmpty

/** An [UpsertMaterialRequest] whose every field has been validated and normalised. */
data class MaterialDraft(
    val title: Title,
    val description: Description,
    val type: MaterialType,
    val url: HttpUrl,
    val areas: NonEmptyList<SocialArea>,
) {
    companion object {
        fun parse(request: UpsertMaterialRequest): EitherNel<FieldError, MaterialDraft> =
            Either.zipOrAccumulate(
                Title.parse(request.title).asNel(),
                Description.parse(request.description).asNel(),
                HttpUrl.parse(request.url, "url").asNel(),
                request.areas.toNonEmpty("areas", "Wymagany jest co najmniej jeden obszar społeczny").asNel(),
            ) { title, description, url, areas ->
                MaterialDraft(title = title, description = description, type = request.type, url = url, areas = areas)
            }
    }
}
