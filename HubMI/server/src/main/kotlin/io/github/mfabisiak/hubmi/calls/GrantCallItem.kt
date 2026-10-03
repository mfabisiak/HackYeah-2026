package io.github.mfabisiak.hubmi.calls

import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.common.mongo.JavaInstantAsBsonDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Clock
import java.time.Instant

@Serializable
data class GrantCallItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val title: String,
    val description: String,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val opensAt: Instant,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val closesAt: Instant,
    val fields: List<CallField>,
    val submittedCounts: Map<String, Int> = emptyMap(),
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val createdAt: Instant,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val updatedAt: Instant,
)

fun GrantCallItem.computeStatus(clock: Clock): CallStatus {
    val now = clock.instant()
    return when {
        now.isBefore(opensAt) -> CallStatus.UPCOMING
        now.isAfter(closesAt) -> CallStatus.CLOSED
        else -> CallStatus.OPEN
    }
}

fun GrantCallItem.toDto(clock: Clock): GrantCallDto =
    GrantCallDto(
        id = id.toHexString(),
        title = title,
        description = description,
        opensAt = opensAt.toString(),
        closesAt = closesAt.toString(),
        status = computeStatus(clock),
        fields = fields,
    )
