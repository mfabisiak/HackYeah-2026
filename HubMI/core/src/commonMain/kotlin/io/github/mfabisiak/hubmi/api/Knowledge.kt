package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 2 (knowledge base): challenges of the region and educational materials.

/** `GET` (public) lists, `POST` (admin) creates. */
@Serializable
@Resource("challenges")
class Challenges(
    val parent: Api = Api(),
    val area: SocialArea? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    /** `GET` (public), `PUT` / `DELETE` (admin). */
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Challenges = Challenges(),
        val id: String,
    )
}

/** `GET` (public) lists, `POST` (admin) creates. */
@Serializable
@Resource("materials")
class Materials(
    val parent: Api = Api(),
    val q: String? = null,
    val area: SocialArea? = null,
    val type: MaterialType? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    /** `GET` (public), `PUT` / `DELETE` (admin). */
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Materials = Materials(),
        val id: String,
    )
}

@Serializable
enum class MaterialType {
    REPORT,
    VIDEO,
    GUIDE,
    CANVAS,
}

@Serializable
data class ChallengeDto(
    val id: String,
    val title: String,
    val description: String,
    val area: SocialArea,
    val municipalities: List<String>,
)

@Serializable
data class UpsertChallengeRequest(
    val title: String,
    val description: String,
    val area: SocialArea,
    val municipalities: List<String> = emptyList(),
)

@Serializable
data class MaterialDto(
    val id: String,
    val title: String,
    val description: String,
    val type: MaterialType,
    val url: String,
    val areas: List<SocialArea>,
)

@Serializable
data class UpsertMaterialRequest(
    val title: String,
    val description: String,
    val type: MaterialType,
    val url: String,
    val areas: List<SocialArea> = emptyList(),
)
