package io.github.mfabisiak.hubmi.tester

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.FeedbackDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.github.mfabisiak.hubmi.innovations.InnovationRepository
import io.github.mfabisiak.hubmi.innovations.innovationNotFound
import java.time.Instant

class FeedbackService(
    private val innovations: InnovationRepository,
    private val feedbacks: FeedbackRepository,
) {
    /**
     * Saves the user's rating (a repeated one replaces the previous) and refreshes the innovation's aggregate. The
     * aggregate is recomputed from the stored ratings rather than adjusted by a delta, so it cannot drift.
     */
    suspend fun submit(
        innovationId: InnovationId,
        userId: UserId,
        draft: FeedbackDraft,
    ): Either<DomainError, FeedbackDto> =
        either {
            ensureNotNull(innovations.findById(innovationId).mapLeft { it.toDomainError() }.bind()) {
                innovationNotFound(innovationId)
            }
            val feedback =
                feedbacks
                    .upsert(innovationId, userId, draft, Instant.now().toString())
                    .mapLeft { it.toDomainError() }
                    .bind()
            refreshRating(innovationId, REFRESH_RETRIES).bind()
            feedback.toDto()
        }

    /** The caller's own rating of the innovation. */
    suspend fun mine(
        innovationId: InnovationId,
        userId: UserId,
    ): Either<DomainError, FeedbackDto> =
        either {
            val feedback =
                ensureNotNull(feedbacks.findMine(innovationId, userId).mapLeft { it.toDomainError() }.bind()) {
                    DomainError.NotFound("Nie oceniono jeszcze tej innowacji")
                }
            feedback.toDto()
        }

    /** Admin view: every rating with its comment, newest first. */
    suspend fun list(
        innovationId: InnovationId?,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<AdminFeedbackDto>> =
        either {
            val page = feedbacks.findPage(innovationId, pageRequest).mapLeft { it.toDomainError() }.bind()
            val titles =
                innovations
                    .titlesOf(page.items.map(FeedbackItem::innovationId).distinct())
                    .mapLeft {
                        it.toDomainError()
                    }.bind()
            Page(
                items = page.items.map { it.toAdminDto(titles[it.innovationId].orEmpty()) },
                page = page.page,
                size = page.size,
                total = page.total,
            )
        }

    /**
     * Writes the aggregate and reads the totals again: if a rating arrived in between, the written value may be stale,
     * so it is written again (a few times at most). Without transactions this narrows the window to practically zero.
     */
    private suspend fun refreshRating(
        innovationId: InnovationId,
        retriesLeft: Int,
    ): Either<DomainError, Unit> =
        either {
            val totals = feedbacks.ratingTotals(innovationId).mapLeft { it.toDomainError() }.bind()
            innovations.setRating(innovationId, totals.sum, totals.count).mapLeft { it.toDomainError() }.bind()
            val after = feedbacks.ratingTotals(innovationId).mapLeft { it.toDomainError() }.bind()
            if (after != totals && retriesLeft > 0) refreshRating(innovationId, retriesLeft - 1).bind()
        }

    private companion object {
        const val REFRESH_RETRIES = 3
    }
}
