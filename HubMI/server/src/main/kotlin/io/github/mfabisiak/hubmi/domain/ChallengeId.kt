package io.github.mfabisiak.hubmi.domain

import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import org.bson.types.ObjectId

@JvmInline
value class ChallengeId(
    val value: ObjectId,
) {
    companion object {
        fun parse(raw: String): EitherNel<FieldError, ChallengeId> = parseObjectId(raw).asNel().map(::ChallengeId)
    }
}
