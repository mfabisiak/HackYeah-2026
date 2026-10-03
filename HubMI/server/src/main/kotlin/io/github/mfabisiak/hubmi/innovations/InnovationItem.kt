package io.github.mfabisiak.hubmi.innovations

import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class InnovationItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val title: String,
    val summary: String,
    val description: String,
    val areas: List<SocialArea>,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val region: String? = null,
    val keywords: List<String> = emptyList(),
    val mediaUrls: List<String> = emptyList(),
    val innovativeness: String? = null,
    val problemDiagnosis: String? = null,
    val audienceDescription: String? = null,
    val expectedChange: String? = null,
    val futureVision: String? = null,
    val ratingSum: Int = 0,
    val ratingsCount: Int = 0,
    val archived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)
