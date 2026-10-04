package io.github.mfabisiak.hubmi.web.mock

import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.FeedbackDto
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.TestRequestDto
import kotlin.js.Date

internal fun InnovationDto.toSummary(): InnovationSummary =
    InnovationSummary(id, title, summary, areas, targetGroups, stage)

internal fun AdminTestRequestDto.toRequest(): TestRequestDto = TestRequestDto(id, innovationId, note, status, createdAt)

internal fun AdminFeedbackDto.toFeedback(): FeedbackDto =
    FeedbackDto(id, innovationId, rating, comment, suggestion, createdAt)

/** The status of a call follows from its dates, so it keeps being right however long the demo's data is kept. */
internal fun GrantCallDto.statusNow(): GrantCallDto {
    val now = Date.now()
    return copy(
        status =
            when {
                epochMillis(opensAt) > now -> CallStatus.UPCOMING
                epochMillis(closesAt) < now -> CallStatus.CLOSED
                else -> CallStatus.OPEN
            },
    )
}

/** What a month-long pilot costs an institution, by the number of people it assigns to it. */
internal fun estimatePilotCostPln(staffCount: Int): Int = FIXED_PILOT_COST_PLN + staffCount * COST_PER_PERSON_PLN

private const val FIXED_PILOT_COST_PLN = 8_000
private const val COST_PER_PERSON_PLN = 7_500
