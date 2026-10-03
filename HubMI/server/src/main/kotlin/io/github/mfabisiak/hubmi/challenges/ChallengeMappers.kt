package io.github.mfabisiak.hubmi.challenges

import io.github.mfabisiak.hubmi.api.ChallengeDto

fun ChallengeItem.toDto(): ChallengeDto =
    ChallengeDto(
        id = id.toHexString(),
        title = title,
        description = description,
        area = area,
        municipalities = municipalities,
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
