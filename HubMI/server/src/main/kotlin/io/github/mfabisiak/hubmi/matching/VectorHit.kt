package io.github.mfabisiak.hubmi.matching

import org.bson.types.ObjectId

data class VectorHit(
    val key: ObjectId,
    val cosine: Double,
)
