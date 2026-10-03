package io.github.mfabisiak.hubmi.challenges

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import java.time.Instant

class ChallengeService(
    private val repository: ChallengeRepository,
) {
    suspend fun list(
        area: SocialArea?,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<ChallengeDto>> =
        either {
            val result =
                repository
                    .findAll(area, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(id: ChallengeId): Either<DomainError, ChallengeDto> =
        either {
            val item =
                ensureNotNull(repository.findById(id).mapLeft { it.toDomainError() }.bind()) {
                    notFound(id)
                }
            item.toDto()
        }

    suspend fun create(draft: ChallengeDraft): Either<DomainError, ChallengeDto> =
        repository
            .create(draft.toItem(Instant.now().toString()))
            .mapLeft { it.toDomainError() }
            .map { it.toDto() }

    suspend fun update(
        id: ChallengeId,
        draft: ChallengeDraft,
    ): Either<DomainError, ChallengeDto> =
        either {
            val updated =
                ensureNotNull(
                    repository.update(id, draft, Instant.now().toString()).mapLeft { it.toDomainError() }.bind(),
                ) { notFound(id) }
            updated.toDto()
        }

    suspend fun delete(id: ChallengeId): Either<DomainError, Unit> =
        either {
            val deleted =
                repository.softDelete(id, Instant.now().toString()).mapLeft { it.toDomainError() }.bind()
            ensure(deleted) { notFound(id) }
        }

    private fun notFound(id: ChallengeId) =
        DomainError.NotFound("Nie znaleziono wyzwania o ID: ${id.value.toHexString()}")
}
