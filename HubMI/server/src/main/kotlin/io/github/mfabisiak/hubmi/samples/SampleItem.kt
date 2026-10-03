package io.github.mfabisiak.hubmi.samples

import io.github.mfabisiak.hubmi.api.SampleDto
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/** Stored in the `samples` collection; encoded by bson-kotlinx (see `SampleRepository.kt`). */
@Serializable
data class SampleItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val slug: String,
    val name: String,
    val description: String,
)

fun SampleItem.toDto(): SampleDto =
    SampleDto(
        id = id.toHexString(),
        slug = slug,
        name = name,
        description = description,
    )
