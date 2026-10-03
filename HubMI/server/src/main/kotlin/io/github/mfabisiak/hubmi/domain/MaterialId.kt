package io.github.mfabisiak.hubmi.domain

import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import org.bson.types.ObjectId

@JvmInline
value class MaterialId(
    val value: ObjectId,
) {
    companion object {
        fun parse(raw: String): EitherNel<FieldError, MaterialId> = parseObjectId(raw).asNel().map(::MaterialId)
    }
}
