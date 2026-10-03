package io.github.mfabisiak.hubmi.challenges

import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.parseObjectId
import org.bson.types.ObjectId

@JvmInline
value class ChallengeId(
    val value: ObjectId,
) {
    companion object {
        fun parse(raw: String): EitherNel<FieldError, ChallengeId> = parseObjectId(raw).asNel().map(::ChallengeId)
    }
}
