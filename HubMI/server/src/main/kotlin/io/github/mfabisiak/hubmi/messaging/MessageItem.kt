package io.github.mfabisiak.hubmi.messaging

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.api.ParticipantRole
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

data class MessageItem(
    @BsonId val id: ObjectId = ObjectId(),
    val threadId: ObjectId,
    val authorId: String,
    val authorName: String,
    val authorRole: ParticipantRole,
    val text: String,
    val sentAt: Instant,
)

val MongoDatabase.messages get() = getCollection<MessageItem>("messages")
