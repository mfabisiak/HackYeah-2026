package io.github.mfabisiak.hubmi.web.admin

import io.github.mfabisiak.hubmi.api.TrendsDto

@JsExport
class AreaTrendJs(
    /** `SocialArea` name. */
    val area: String,
    val count: Int,
    val previousCount: Int,
)

@JsExport
class MunicipalityTrendJs(
    val municipality: String,
    val count: Int,
)

@JsExport
class TrendsJs(
    val byArea: Array<AreaTrendJs>,
    val byMunicipality: Array<MunicipalityTrendJs>,
    val unmatchedNeeds: Int,
)

internal fun TrendsDto.toJs(): TrendsJs =
    TrendsJs(
        byArea = byArea.map { AreaTrendJs(it.area.name, it.count, it.previousCount) }.toTypedArray(),
        byMunicipality = byMunicipality.map { MunicipalityTrendJs(it.municipality, it.count) }.toTypedArray(),
        unmatchedNeeds = unmatchedNeeds,
    )
