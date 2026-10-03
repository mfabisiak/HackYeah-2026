package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.TargetGroup
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class IdeaItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val authorId: String,
    val title: String,
    val essence: String,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val status: IdeaStatus,
    val adminComment: String? = null,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val createdAt: Instant,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val updatedAt: Instant,
)

fun IdeaItem.toDto(): IdeaDto =
    IdeaDto(
        id = id.toHexString(),
        title = title,
        essence = essence,
        targetGroups = targetGroups,
        stage = stage,
        status = status,
        adminComment = adminComment,
        createdAt = createdAt.toString(),
    )
