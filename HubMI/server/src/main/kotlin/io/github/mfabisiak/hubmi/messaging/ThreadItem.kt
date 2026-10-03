package io.github.mfabisiak.hubmi.messaging

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.api.ParticipantRole
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

data class ThreadItem(
    @BsonId val id: ObjectId = ObjectId(),
    val ownerId: String,
    val ownerName: String,
    val subject: String,
    val relatedIdeaId: ObjectId? = null,
    val participantIds: Set<String> = setOf(ownerId),
    val lastMessageAt: Instant,
    val lastMessageBy: String,
    val lastMessageRole: ParticipantRole,
    val lastReadAt: Map<String, Instant> = emptyMap(),
    val createdAt: Instant,
    val updatedAt: Instant,
)

val MongoDatabase.threads get() = getCollection<ThreadItem>("threads")
