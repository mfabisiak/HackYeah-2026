package io.github.mfabisiak.hubmi.messaging

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.Role
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

data class NotificationItem(
    @BsonId val id: ObjectId = ObjectId(),
    val recipientId: String? = null,
    val targetRole: Role? = null,
    val readByUserIds: Set<String> = emptySet(),
    val type: NotificationType,
    val title: String,
    val body: String,
    val read: Boolean = false,
    val eventId: String? = null,
    val createdAt: Instant,
)

val MongoDatabase.notifications get() = getCollection<NotificationItem>("notifications")
