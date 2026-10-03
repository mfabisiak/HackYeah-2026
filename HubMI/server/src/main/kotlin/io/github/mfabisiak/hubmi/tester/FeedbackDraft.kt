package io.github.mfabisiak.hubmi.tester

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.CreateFeedbackRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.asNel

/** A [CreateFeedbackRequest] whose every field has been validated and normalised. */
data class FeedbackDraft(
    val rating: Rating,
    val comment: Comment?,
    val suggestion: Comment?,
) {
    companion object {
        fun parse(request: CreateFeedbackRequest): EitherNel<FieldError, FeedbackDraft> =
            Either.zipOrAccumulate(
                Rating.parse(request.rating).asNel(),
                Comment.parse(request.comment, "comment").asNel(),
                Comment.parse(request.suggestion, "suggestion").asNel(),
            ) { rating, comment, suggestion -> FeedbackDraft(rating, comment, suggestion) }
    }
}
