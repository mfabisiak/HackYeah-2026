package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.ApplicationDto
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class ApplicationItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    @Contextual
    val callId: ObjectId,
    val applicantId: String,
    @Contextual
    val ideaId: ObjectId? = null,
    val answers: Map<String, String>,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val createdAt: Instant,
)

fun ApplicationItem.toDto(): ApplicationDto =
    ApplicationDto(
        id = id.toHexString(),
        callId = callId.toHexString(),
        ideaId = ideaId?.toHexString(),
        createdAt = createdAt.toString(),
    )
