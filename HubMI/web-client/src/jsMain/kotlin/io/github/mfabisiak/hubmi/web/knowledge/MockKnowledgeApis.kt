package io.github.mfabisiak.hubmi.web.knowledge

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.mock.stemsOf
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlin.js.Promise

internal class MockChallengesApi(
    private val backend: MockBackend,
) : ChallengesApi {
    private val store = backend.store

    override fun list(
        area: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<ChallengeJs>>> =
        backend.respond {
            val areaFilter = enumOrNull<SocialArea>(area, "area")
            store.db.challenges
                .filter { areaFilter == null || it.area == areaFilter }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun get(id: String): Promise<ApiResult<ChallengeJs>> =
        backend.respond {
            ensureNotNull(store.db.challenges.firstOrNull { it.id == id }) { notFound("wyzwanie $id") }.toJs()
        }

    override fun create(request: UpsertChallengeJs): Promise<ApiResult<ChallengeJs>> =
        backend.respond {
            val challenge =
                request
                    .toDto()
                    .flatMap { it.validated() }
                    .bind()
                    .toChallenge(newId("wyzwanie"))
            store.update { it.copy(challenges = listOf(challenge) + it.challenges) }
            challenge.toJs()
        }

    override fun update(
        id: String,
        request: UpsertChallengeJs,
    ): Promise<ApiResult<ChallengeJs>> =
        backend.respond {
            ensure(store.db.challenges.any { it.id == id }) { notFound("wyzwanie $id") }
            val challenge =
                request
                    .toDto()
                    .flatMap { it.validated() }
                    .bind()
                    .toChallenge(id)
            store.update { db -> db.copy(challenges = db.challenges.map { if (it.id == id) challenge else it }) }
            challenge.toJs()
        }

    override fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        backend.respond {
            ensure(store.db.challenges.any { it.id == id }) { notFound("wyzwanie $id") }
            store.update { db -> db.copy(challenges = db.challenges.filterNot { it.id == id }) }
            EmptyJs()
        }

    private fun UpsertChallengeRequest.validated(): Either<ApiErrorJs, UpsertChallengeRequest> {
        val errors =
            listOfNotNull(
                blank("title").takeIf { title.isBlank() },
                blank("description").takeIf { description.isBlank() },
            )
        return if (errors.isEmpty()) right() else invalid(*errors.toTypedArray()).left()
    }

    private fun UpsertChallengeRequest.toChallenge(id: String) =
        ChallengeDto(id, title.trim(), description.trim(), area, municipalities)
}

internal class MockMaterialsApi(
    private val backend: MockBackend,
) : MaterialsApi {
    private val store = backend.store

    override fun list(
        q: String?,
        area: String?,
        type: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<MaterialJs>>> =
        backend.respond {
            val areaFilter = enumOrNull<SocialArea>(area, "area")
            val typeFilter = enumOrNull<MaterialType>(type, "type")
            val queryStems = q?.let(::stemsOf).orEmpty()
            store.db.materials
                .filter { areaFilter == null || areaFilter in it.areas }
                .filter { typeFilter == null || it.type == typeFilter }
                .filter { queryStems.isEmpty() || stemsOf(it.title + " " + it.description).containsAll(queryStems) }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun get(id: String): Promise<ApiResult<MaterialJs>> =
        backend.respond {
            ensureNotNull(store.db.materials.firstOrNull { it.id == id }) { notFound("materiał $id") }.toJs()
        }

    override fun create(request: UpsertMaterialJs): Promise<ApiResult<MaterialJs>> =
        backend.respond {
            val material =
                request
                    .toDto()
                    .flatMap { it.validated() }
                    .bind()
                    .toMaterial(newId("material"))
            store.update { it.copy(materials = listOf(material) + it.materials) }
            material.toJs()
        }

    override fun update(
        id: String,
        request: UpsertMaterialJs,
    ): Promise<ApiResult<MaterialJs>> =
        backend.respond {
            ensure(store.db.materials.any { it.id == id }) { notFound("materiał $id") }
            val material =
                request
                    .toDto()
                    .flatMap { it.validated() }
                    .bind()
                    .toMaterial(id)
            store.update { db -> db.copy(materials = db.materials.map { if (it.id == id) material else it }) }
            material.toJs()
        }

    override fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        backend.respond {
            ensure(store.db.materials.any { it.id == id }) { notFound("materiał $id") }
            store.update { db -> db.copy(materials = db.materials.filterNot { it.id == id }) }
            EmptyJs()
        }

    private fun UpsertMaterialRequest.validated(): Either<ApiErrorJs, UpsertMaterialRequest> {
        val errors =
            listOfNotNull(
                blank("title").takeIf { title.isBlank() },
                blank("description").takeIf { description.isBlank() },
                blank("url").takeIf { url.isBlank() },
            )
        return if (errors.isEmpty()) right() else invalid(*errors.toTypedArray()).left()
    }

    private fun UpsertMaterialRequest.toMaterial(id: String) =
        MaterialDto(id, title.trim(), description.trim(), type, url.trim(), areas)
}
