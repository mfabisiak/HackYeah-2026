package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/** Exemplary resource endpoint: `/api/samples`. */
@Serializable
@Resource("samples")
class Samples(
    val parent: Api = Api(),
    val page: Int = 0,
    val size: Int = 20,
) {
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Samples = Samples(),
        val id: String,
    )
}

@Serializable
data class SampleDto(
    val id: String,
    val slug: String,
    val name: String,
    val description: String,
)

@Serializable
data class CreateSampleRequest(
    val slug: String,
    val name: String,
    val description: String,
)
