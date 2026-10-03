package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

// Module 5 (communication platform) and notifications.

/** `GET` / `POST` (authenticated): conversations of the caller. */
@Serializable
@Resource("threads")
class Threads(
    val parent: Api = Api(),
    val page: Int = 0,
    val size: Int = 20,
) {
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Threads = Threads(),
        val id: String,
    ) {
        /** `GET` lists, `POST` (authenticated, participant) posts a message. */
        @Serializable
        @Resource("messages")
        class Messages(
            val parent: ById,
        )
    }
}

/** `GET` (authenticated): notifications of the caller. */
@Serializable
@Resource("notifications")
class Notifications(
    val parent: Api = Api(),
    val unreadOnly: Boolean = false,
    val page: Int = 0,
    val size: Int = 20,
) {
    /** `POST` (authenticated): mark as read. */
    @Serializable
    @Resource("{id}/read")
    class Read(
        val parent: Notifications = Notifications(),
        val id: String,
    )
}

@Serializable
enum class ParticipantRole {
    AUTHOR,
    ADMIN,
    EXPERT,
}

@Serializable
enum class NotificationType {
    IDEA_SUBMITTED,
    IDEA_STATUS_CHANGED,
    CALL_PUBLISHED,
    CALL_CHANGED,
    MESSAGE_RECEIVED,
}

@Serializable
data class CreateThreadRequest(
    val subject: String,
    val message: String,
    val relatedIdeaId: String? = null,
)

@Serializable
data class ThreadDto(
    val id: String,
    val subject: String,
    val relatedIdeaId: String?,
    val lastMessageAt: String,
    val unread: Boolean,
)

@Serializable
data class PostMessageRequest(
    val text: String,
)

@Serializable
data class MessageDto(
    val id: String,
    val authorName: String,
    val authorRole: ParticipantRole,
    val text: String,
    val sentAt: String,
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean,
)
