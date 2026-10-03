package io.github.mfabisiak.hubmi.tester

import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.parseObjectId
import org.bson.types.ObjectId

@JvmInline
value class TestRequestId(
    val value: ObjectId,
) {
    companion object {
        fun parse(raw: String): EitherNel<FieldError, TestRequestId> = parseObjectId(raw).asNel().map(::TestRequestId)
    }
}
