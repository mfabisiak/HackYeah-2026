package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.api.SimilarNeedDto

fun NeedItem.toSimilarNeed(excerpt: String): SimilarNeedDto =
    SimilarNeedDto(id = id.toHexString(), excerpt = excerpt, area = areas.firstOrNull())
