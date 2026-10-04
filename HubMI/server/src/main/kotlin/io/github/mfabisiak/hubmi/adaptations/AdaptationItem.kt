package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.AdaptationStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/** A plan of the Middleman for one innovation and one institution, with its review. */
@Serializable
data class AdaptationItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    @Contextual
    val innovationId: ObjectId,
    val authorId: String,
    val institution: Institution,
    val plan: AdaptationPlan,
    /** The model that wrote [plan]. */
    val model: String,
    val status: AdaptationStatus = AdaptationStatus.PENDING_REVIEW,
    val adminComment: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
