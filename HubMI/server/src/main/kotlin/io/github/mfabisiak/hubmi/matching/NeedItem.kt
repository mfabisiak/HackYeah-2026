package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.api.SocialArea
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/**
 * A problem described by a user. [text] is the description after personal data removal. [ownerId] is the Keycloak
 * subject of a logged-in reporter (so they can be notified and invited to tests); anonymous needs have none.
 */
@Serializable
data class NeedItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val text: String,
    val ownerId: String? = null,
    val municipality: String? = null,
    val areas: List<SocialArea> = emptyList(),
    val matchedInnovationIds: List<@Contextual ObjectId> = emptyList(),
    val noGoodMatch: Boolean,
    val helpful: Boolean? = null,
    val createdAt: String,
)
