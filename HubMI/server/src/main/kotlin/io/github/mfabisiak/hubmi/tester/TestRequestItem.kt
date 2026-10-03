package io.github.mfabisiak.hubmi.tester

import io.github.mfabisiak.hubmi.api.TestRequestStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/** A user's declared willingness to test an innovation; one per ([innovationId], [userId]). */
@Serializable
data class TestRequestItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    @Contextual
    val innovationId: ObjectId,
    val userId: String,
    val note: String? = null,
    val status: TestRequestStatus = TestRequestStatus.NEW,
    val createdAt: String,
    val updatedAt: String,
)
