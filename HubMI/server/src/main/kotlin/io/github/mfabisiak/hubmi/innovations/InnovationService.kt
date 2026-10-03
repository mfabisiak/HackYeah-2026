package io.github.mfabisiak.hubmi.innovations

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.SearchQuery
import io.github.mfabisiak.hubmi.common.toDomainError
import java.time.Instant

class InnovationService(
    private val repository: InnovationRepository,
) {
    suspend fun list(
        q: SearchQuery?,
        area: SocialArea?,
        targetGroup: TargetGroup?,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<InnovationSummary>> =
        either {
            val result =
                repository
                    .findAll(q, area, targetGroup, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toSummary() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(id: InnovationId): Either<DomainError, InnovationDto> =
        either {
            val item =
                ensureNotNull(repository.findById(id).mapLeft { it.toDomainError() }.bind()) {
                    notFound(id)
                }
            item.toDto()
        }

    suspend fun create(draft: InnovationDraft): Either<DomainError, InnovationDto> =
        repository
            .create(draft.toItem(Instant.now().toString()))
            .mapLeft { it.toDomainError() }
            .map { it.toDto() }

    suspend fun update(
        id: InnovationId,
        draft: InnovationDraft,
    ): Either<DomainError, InnovationDto> =
        either {
            val updated =
                ensureNotNull(
                    repository.update(id, draft, Instant.now().toString()).mapLeft { it.toDomainError() }.bind(),
                ) { notFound(id) }
            updated.toDto()
        }

    suspend fun delete(id: InnovationId): Either<DomainError, Unit> =
        either {
            val deleted =
                repository.softDelete(id, Instant.now().toString()).mapLeft { it.toDomainError() }.bind()
            ensure(deleted) { notFound(id) }
        }

    private fun notFound(id: InnovationId) =
        DomainError.NotFound("Nie znaleziono innowacji o ID: ${id.value.toHexString()}")
}
