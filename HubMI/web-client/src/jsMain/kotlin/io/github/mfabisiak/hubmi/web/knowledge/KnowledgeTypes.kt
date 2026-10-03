package io.github.mfabisiak.hubmi.web.knowledge

import arrow.core.Either
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumsOf
import io.github.mfabisiak.hubmi.web.names

@JsExport
class ChallengeJs(
    val id: String,
    val title: String,
    val description: String,
    /** `SocialArea` name. */
    val area: String,
    val municipalities: Array<String>,
)

@JsExport
class UpsertChallengeJs(
    val title: String,
    val description: String,
    val area: String,
    val municipalities: Array<String> = emptyArray(),
)

@JsExport
class MaterialJs(
    val id: String,
    val title: String,
    val description: String,
    /** `MaterialType` name. */
    val type: String,
    val url: String,
    /** `SocialArea` names. */
    val areas: Array<String>,
)

@JsExport
class UpsertMaterialJs(
    val title: String,
    val description: String,
    val type: String,
    val url: String,
    val areas: Array<String> = emptyArray(),
)

internal fun ChallengeDto.toJs(): ChallengeJs =
    ChallengeJs(id, title, description, area.name, municipalities.toTypedArray())

internal fun MaterialDto.toJs(): MaterialJs = MaterialJs(id, title, description, type.name, url, areas.names())

internal fun UpsertChallengeJs.toDto(): Either<ApiErrorJs, UpsertChallengeRequest> =
    either {
        UpsertChallengeRequest(
            title = title,
            description = description,
            area = enumOf<SocialArea>(area, "area"),
            municipalities = municipalities.toList(),
        )
    }

internal fun UpsertMaterialJs.toDto(): Either<ApiErrorJs, UpsertMaterialRequest> =
    either {
        UpsertMaterialRequest(
            title = title,
            description = description,
            type = enumOf<MaterialType>(type, "type"),
            url = url,
            areas = enumsOf<SocialArea>(areas, "areas"),
        )
    }
