package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MeResponse
import io.github.mfabisiak.hubmi.domain.ChallengeDraft
import io.github.mfabisiak.hubmi.domain.HttpUrl
import io.github.mfabisiak.hubmi.domain.InnovationDraft
import io.github.mfabisiak.hubmi.domain.MaterialDraft
import io.github.mfabisiak.hubmi.domain.MunicipalityName
import io.github.mfabisiak.hubmi.plugins.realmRoles
import io.ktor.server.auth.jwt.*

fun JWTPrincipal.toMeResponse(): MeResponse =
    MeResponse(
        id = subject,
        username = payload.getClaim("preferred_username").asString(),
        email = payload.getClaim("email").asString(),
        roles = realmRoles,
    )

fun InnovationItem.toSummary(): InnovationSummary =
    InnovationSummary(
        id = id.toHexString(),
        title = title,
        summary = summary,
        areas = areas,
        targetGroups = targetGroups,
        stage = stage,
    )

fun InnovationItem.toDto(): InnovationDto =
    InnovationDto(
        id = id.toHexString(),
        title = title,
        summary = summary,
        description = description,
        areas = areas,
        targetGroups = targetGroups,
        stage = stage,
        region = region,
        mediaUrls = mediaUrls,
        averageRating = if (ratingsCount > 0) ratingSum.toDouble() / ratingsCount else null,
        ratingsCount = ratingsCount,
    )

fun ChallengeItem.toDto(): ChallengeDto =
    ChallengeDto(
        id = id.toHexString(),
        title = title,
        description = description,
        area = area,
        municipalities = municipalities,
    )

fun MaterialItem.toDto(): MaterialDto =
    MaterialDto(
        id = id.toHexString(),
        title = title,
        description = description,
        type = type,
        url = url,
        areas = areas,
    )

fun InnovationDraft.toItem(now: String): InnovationItem =
    InnovationItem(
        title = title.value,
        summary = summary.value,
        description = description.value,
        areas = areas.toList(),
        targetGroups = targetGroups.toList(),
        stage = stage,
        region = region?.value,
        mediaUrls = mediaUrls.map(HttpUrl::value),
        createdAt = now,
        updatedAt = now,
    )

fun ChallengeDraft.toItem(now: String): ChallengeItem =
    ChallengeItem(
        title = title.value,
        description = description.value,
        area = area,
        municipalities = municipalities.map(MunicipalityName::value),
        createdAt = now,
        updatedAt = now,
    )

fun MaterialDraft.toItem(now: String): MaterialItem =
    MaterialItem(
        title = title.value,
        description = description.value,
        type = type,
        url = url.value,
        areas = areas.toList(),
        createdAt = now,
        updatedAt = now,
    )
