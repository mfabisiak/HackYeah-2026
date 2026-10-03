package io.github.mfabisiak.hubmi.tester

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.TestRequestDto
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.github.mfabisiak.hubmi.innovations.InnovationRepository
import io.github.mfabisiak.hubmi.innovations.innovationNotFound
import java.time.Instant

class TestRequestService(
    private val innovations: InnovationRepository,
    private val testRequests: TestRequestRepository,
) {
    /** Idempotent: repeating it replaces the note, until an admin decides on the request (then `409`). */
    suspend fun submit(
        innovationId: InnovationId,
        userId: UserId,
        draft: TestRequestDraft,
    ): Either<DomainError, TestRequestDto> =
        either {
            ensureNotNull(innovations.findById(innovationId).mapLeft { it.toDomainError() }.bind()) {
                innovationNotFound(innovationId)
            }
            testRequests
                .upsert(innovationId, userId, draft, Instant.now().toString())
                .mapLeft { error ->
                    when (error) {
                        is RepositoryError.Conflict -> {
                            DomainError.Conflict(
                                "Zgłoszenie zostało już rozpatrzone i nie można go zmienić",
                            )
                        }

                        is RepositoryError.DatabaseException -> {
                            error.toDomainError()
                        }
                    }
                }.bind()
                .toDto()
        }

    /** The caller's own request, with the admin's decision once there is one. */
    suspend fun mine(
        innovationId: InnovationId,
        userId: UserId,
    ): Either<DomainError, TestRequestDto> =
        either {
            val request =
                ensureNotNull(testRequests.findMine(innovationId, userId).mapLeft { it.toDomainError() }.bind()) {
                    DomainError.NotFound("Nie zgłoszono jeszcze chęci testowania tej innowacji")
                }
            request.toDto()
        }

    suspend fun list(
        innovationId: InnovationId?,
        status: TestRequestStatus?,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<AdminTestRequestDto>> =
        either {
            val page = testRequests.findPage(innovationId, status, pageRequest).mapLeft { it.toDomainError() }.bind()
            val titles =
                innovations
                    .titlesOf(
                        page.items.map(TestRequestItem::innovationId).distinct(),
                    ).mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = page.items.map { it.toAdminDto(titles[it.innovationId].orEmpty()) },
                page = page.page,
                size = page.size,
                total = page.total,
            )
        }

    /** The decision is one atomic update conditioned on the status we checked, so two admins cannot both decide. */
    suspend fun decide(
        id: TestRequestId,
        next: TestRequestStatus,
    ): Either<DomainError, AdminTestRequestDto> =
        either {
            val current =
                ensureNotNull(testRequests.findById(id).mapLeft { it.toDomainError() }.bind()) {
                    DomainError.NotFound("Nie znaleziono zgłoszenia do testów o ID: ${id.value.toHexString()}")
                }
            ensure(current.status.canMoveTo(next)) {
                DomainError.Conflict("Zgłoszenie ma status ${current.status} i nie można go zmienić na $next")
            }
            val decided =
                ensureNotNull(
                    testRequests
                        .transition(
                            id,
                            current.status,
                            next,
                            Instant.now().toString(),
                        ).mapLeft { it.toDomainError() }
                        .bind(),
                ) { DomainError.Conflict("Status zgłoszenia zmienił się w międzyczasie") }
            val titles = innovations.titlesOf(listOf(decided.innovationId)).mapLeft { it.toDomainError() }.bind()
            decided.toAdminDto(titles[decided.innovationId].orEmpty())
        }
}
