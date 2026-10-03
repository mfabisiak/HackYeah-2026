package io.github.mfabisiak.hubmi.materials

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.SearchQuery
import io.github.mfabisiak.hubmi.common.toDomainError
import java.time.Instant

class MaterialService(
    private val repository: MaterialRepository,
) {
    suspend fun list(
        q: SearchQuery?,
        area: SocialArea?,
        type: MaterialType?,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<MaterialDto>> =
        either {
            val result =
                repository
                    .findAll(q, area, type, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(id: MaterialId): Either<DomainError, MaterialDto> =
        either {
            val item =
                ensureNotNull(repository.findById(id).mapLeft { it.toDomainError() }.bind()) {
                    notFound(id)
                }
            item.toDto()
        }

    suspend fun create(draft: MaterialDraft): Either<DomainError, MaterialDto> =
        repository
            .create(draft.toItem(Instant.now().toString()))
            .mapLeft { it.toDomainError() }
            .map { it.toDto() }

    suspend fun update(
        id: MaterialId,
        draft: MaterialDraft,
    ): Either<DomainError, MaterialDto> =
        either {
            val updated =
                ensureNotNull(
                    repository.update(id, draft, Instant.now().toString()).mapLeft { it.toDomainError() }.bind(),
                ) { notFound(id) }
            updated.toDto()
        }

    suspend fun delete(id: MaterialId): Either<DomainError, Unit> =
        either {
            val deleted =
                repository.softDelete(id, Instant.now().toString()).mapLeft { it.toDomainError() }.bind()
            ensure(deleted) { notFound(id) }
        }

    private fun notFound(id: MaterialId) =
        DomainError.NotFound("Nie znaleziono materiału o ID: ${id.value.toHexString()}")
}
