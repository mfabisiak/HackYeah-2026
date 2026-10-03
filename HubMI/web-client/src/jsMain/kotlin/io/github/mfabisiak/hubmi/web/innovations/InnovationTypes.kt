package io.github.mfabisiak.hubmi.web.innovations

import arrow.core.Either
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.FeedbackDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestDto
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumsOf
import io.github.mfabisiak.hubmi.web.names

@JsExport
class InnovationSummaryJs(
    val id: String,
    val title: String,
    val summary: String,
    /** `SocialArea` names. */
    val areas: Array<String>,
    /** `TargetGroup` names. */
    val targetGroups: Array<String>,
    /** `InnovationStage` name. */
    val stage: String,
)

@JsExport
class InnovationJs(
    val id: String,
    val title: String,
    val summary: String,
    val description: String,
    val areas: Array<String>,
    val targetGroups: Array<String>,
    val stage: String,
    val region: String?,
    val mediaUrls: Array<String>,
    val averageRating: Double?,
    val ratingsCount: Int,
)

@JsExport
class UpsertInnovationJs(
    val title: String,
    val summary: String,
    val description: String,
    val areas: Array<String>,
    val targetGroups: Array<String>,
    val stage: String,
    val region: String? = null,
    val mediaUrls: Array<String> = emptyArray(),
)

@JsExport
class TestRequestJs(
    val id: String,
    val innovationId: String,
    val note: String?,
    val createdAt: String,
)

@JsExport
class FeedbackJs(
    val id: String,
    val innovationId: String,
    val rating: Int,
    val comment: String?,
    val suggestion: String?,
    val createdAt: String,
)

internal fun InnovationSummary.toJs(): InnovationSummaryJs =
    InnovationSummaryJs(id, title, summary, areas.names(), targetGroups.names(), stage.name)

internal fun InnovationDto.toJs(): InnovationJs =
    InnovationJs(
        id,
        title,
        summary,
        description,
        areas.names(),
        targetGroups.names(),
        stage.name,
        region,
        mediaUrls.toTypedArray(),
        averageRating,
        ratingsCount,
    )

internal fun TestRequestDto.toJs(): TestRequestJs = TestRequestJs(id, innovationId, note, createdAt)

internal fun FeedbackDto.toJs(): FeedbackJs = FeedbackJs(id, innovationId, rating, comment, suggestion, createdAt)

internal fun UpsertInnovationJs.toDto(): Either<ApiErrorJs, UpsertInnovationRequest> =
    either {
        UpsertInnovationRequest(
            title = title,
            summary = summary,
            description = description,
            areas = enumsOf<SocialArea>(areas, "areas"),
            targetGroups = enumsOf<TargetGroup>(targetGroups, "targetGroups"),
            stage = enumOf<InnovationStage>(stage, "stage"),
            region = region,
            mediaUrls = mediaUrls.toList(),
        )
    }
