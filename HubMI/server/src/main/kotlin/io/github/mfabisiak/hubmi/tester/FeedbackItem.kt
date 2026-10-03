package io.github.mfabisiak.hubmi.tester

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/** One rating per ([innovationId], [userId]); re-rating overwrites it. */
@Serializable
data class FeedbackItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    @Contextual
    val innovationId: ObjectId,
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    val suggestion: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
