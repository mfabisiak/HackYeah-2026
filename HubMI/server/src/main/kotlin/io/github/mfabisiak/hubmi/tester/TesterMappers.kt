package io.github.mfabisiak.hubmi.tester

import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.FeedbackDto
import io.github.mfabisiak.hubmi.api.TestRequestDto

fun FeedbackItem.toDto(): FeedbackDto =
    FeedbackDto(
        id = id.toHexString(),
        innovationId = innovationId.toHexString(),
        rating = rating,
        comment = comment,
        suggestion = suggestion,
        createdAt = createdAt,
    )

fun TestRequestItem.toDto(): TestRequestDto =
    TestRequestDto(
        id = id.toHexString(),
        innovationId = innovationId.toHexString(),
        note = note,
        status = status,
        createdAt = createdAt,
    )

fun FeedbackItem.toAdminDto(innovationTitle: String): AdminFeedbackDto =
    AdminFeedbackDto(
        id = id.toHexString(),
        innovationId = innovationId.toHexString(),
        innovationTitle = innovationTitle,
        userId = userId,
        rating = rating,
        comment = comment,
        suggestion = suggestion,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun TestRequestItem.toAdminDto(innovationTitle: String): AdminTestRequestDto =
    AdminTestRequestDto(
        id = id.toHexString(),
        innovationId = innovationId.toHexString(),
        innovationTitle = innovationTitle,
        userId = userId,
        note = note,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
