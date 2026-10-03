package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MaterialItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val title: String,
    val description: String,
    val type: MaterialType,
    val url: String,
    val areas: List<SocialArea> = emptyList(),
    val archived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)
