package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MeResponse
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
