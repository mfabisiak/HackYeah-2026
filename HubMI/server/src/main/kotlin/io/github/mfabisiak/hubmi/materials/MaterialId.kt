package io.github.mfabisiak.hubmi.materials

import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.parseObjectId
import org.bson.types.ObjectId

@JvmInline
value class MaterialId(
    val value: ObjectId,
) {
    companion object {
        fun parse(raw: String): EitherNel<FieldError, MaterialId> = parseObjectId(raw).asNel().map(::MaterialId)
    }
}
