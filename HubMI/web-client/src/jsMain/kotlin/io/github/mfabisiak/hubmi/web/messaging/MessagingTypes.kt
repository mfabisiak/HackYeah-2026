package io.github.mfabisiak.hubmi.web.messaging

import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.ReplyTemplateDto
import io.github.mfabisiak.hubmi.api.ThreadDto

@JsExport
class ThreadJs(
    val id: String,
    val subject: String,
    val relatedIdeaId: String?,
    val lastMessageAt: String,
    val unread: Boolean,
    /** Name of the ROPS official who handles the thread, `null` while nobody does. */
    val assigneeName: String?,
    val assignedToMe: Boolean,
)

@JsExport
class ReplyTemplateJs(
    val id: String,
    val title: String,
    val text: String,
)

@JsExport
class MessageJs(
    val id: String,
    val authorName: String,
    /** `ParticipantRole` name. */
    val authorRole: String,
    val text: String,
    val sentAt: String,
)

@JsExport
class NotificationJs(
    val id: String,
    /** `NotificationType` name. */
    val type: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean,
)

internal fun ThreadDto.toJs(): ThreadJs =
    ThreadJs(id, subject, relatedIdeaId, lastMessageAt, unread, assigneeName, assignedToMe)

internal fun ReplyTemplateDto.toJs(): ReplyTemplateJs = ReplyTemplateJs(id, title, text)

internal fun MessageDto.toJs(): MessageJs = MessageJs(id, authorName, authorRole.name, text, sentAt)

internal fun NotificationDto.toJs(): NotificationJs = NotificationJs(id, type.name, title, body, createdAt, read)
