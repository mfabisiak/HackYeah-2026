package io.github.mfabisiak.hubmi.messaging

import arrow.core.EitherNel
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import org.bson.types.ObjectId

data class ValidatedCreateThread(
    val subject: String,
    val initialMessage: String,
    val relatedIdeaId: ObjectId?,
)

data class ValidatedPostMessage(
    val text: String,
)

object ThreadDraft {
    const val MAX_SUBJECT_LENGTH = 200
    const val MAX_MESSAGE_LENGTH = 2000

    fun parseCreate(request: CreateThreadRequest): EitherNel<FieldError, ValidatedCreateThread> {
        val errors =
            buildList {
                val trimmedSubject = request.subject.trim()
                if (trimmedSubject.isBlank()) {
                    add(FieldError("subject", FieldErrorCode.Blank, "Temat wątku nie może być pusty"))
                } else if (trimmedSubject.length > MAX_SUBJECT_LENGTH) {
                    add(
                        FieldError(
                            "subject",
                            FieldErrorCode.Range(1, MAX_SUBJECT_LENGTH),
                            "Temat wątku nie może przekraczać $MAX_SUBJECT_LENGTH znaków",
                        ),
                    )
                }

                val trimmedMessage = request.message.trim()
                if (trimmedMessage.isBlank()) {
                    add(FieldError("message", FieldErrorCode.Blank, "Treść wiadomości nie może być pusta"))
                } else if (trimmedMessage.length > MAX_MESSAGE_LENGTH) {
                    add(
                        FieldError(
                            "message",
                            FieldErrorCode.Range(1, MAX_MESSAGE_LENGTH),
                            "Treść wiadomości nie może przekraczać $MAX_MESSAGE_LENGTH znaków",
                        ),
                    )
                }

                if (request.relatedIdeaId != null && !ObjectId.isValid(request.relatedIdeaId)) {
                    add(
                        FieldError(
                            "relatedIdeaId",
                            FieldErrorCode.InvalidFormat,
                            "Nieprawidłowy format identyfikatora pomysłu",
                        ),
                    )
                }
            }

        val nel = errors.toNonEmptyListOrNull()
        return if (nel != null) {
            nel.left()
        } else {
            ValidatedCreateThread(
                subject = request.subject.trim(),
                initialMessage = request.message.trim(),
                relatedIdeaId = request.relatedIdeaId?.let { ObjectId(it) },
            ).right()
        }
    }

    fun parseMessage(request: PostMessageRequest): EitherNel<FieldError, ValidatedPostMessage> {
        val trimmedText = request.text.trim()
        val errors =
            buildList {
                if (trimmedText.isBlank()) {
                    add(FieldError("text", FieldErrorCode.Blank, "Treść wiadomości nie może być pusta"))
                } else if (trimmedText.length > MAX_MESSAGE_LENGTH) {
                    add(
                        FieldError(
                            "text",
                            FieldErrorCode.Range(1, MAX_MESSAGE_LENGTH),
                            "Treść wiadomości nie może przekraczać $MAX_MESSAGE_LENGTH znaków",
                        ),
                    )
                }
            }

        val nel = errors.toNonEmptyListOrNull()
        return if (nel != null) {
            nel.left()
        } else {
            ValidatedPostMessage(
                text = trimmedText,
            ).right()
        }
    }
}
