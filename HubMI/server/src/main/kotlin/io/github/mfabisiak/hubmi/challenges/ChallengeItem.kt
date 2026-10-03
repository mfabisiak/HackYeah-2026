package io.github.mfabisiak.hubmi.challenges

import io.github.mfabisiak.hubmi.api.SocialArea
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class ChallengeItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val title: String,
    val description: String,
    val area: SocialArea,
    val municipalities: List<String> = emptyList(),
    val archived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)
