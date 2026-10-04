package io.github.mfabisiak.hubmi.messaging

import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.ThreadDto

fun ThreadItem.toDto(callerId: String): ThreadDto {
    val lastRead = lastReadAt[callerId]
    val isUnread =
        if (lastMessageBy == callerId) {
            false
        } else {
            lastRead == null || lastRead.isBefore(lastMessageAt)
        }
    return ThreadDto(
        id = id.toHexString(),
        subject = subject,
        relatedIdeaId = relatedIdeaId?.toHexString(),
        lastMessageAt = lastMessageAt.toString(),
        unread = isUnread,
        assigneeName = assigneeName,
        assignedToMe = assigneeId == callerId,
    )
}

fun MessageItem.toDto(): MessageDto =
    MessageDto(
        id = id.toHexString(),
        authorName = authorName,
        authorRole = authorRole,
        text = text,
        sentAt = sentAt.toString(),
    )

fun NotificationItem.toDto(callerId: String): NotificationDto {
    val isRead =
        if (targetRole != null) {
            callerId in readByUserIds
        } else {
            read
        }
    return NotificationDto(
        id = id.toHexString(),
        type = type,
        title = title,
        body = body,
        createdAt = createdAt.toString(),
        read = isRead,
    )
}
