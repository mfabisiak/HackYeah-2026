package io.github.mfabisiak.hubmi.tester

import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.CreateTestRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.asNel

/** A [CreateTestRequest] whose note has been validated and normalised. */
data class TestRequestDraft(
    val note: Comment?,
) {
    companion object {
        fun parse(request: CreateTestRequest): EitherNel<FieldError, TestRequestDraft> =
            Comment.parse(request.note, "note").asNel().map(::TestRequestDraft)
    }
}
