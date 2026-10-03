package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class MessageRepository(
    database: MongoDatabase,
) {
    private val collection = database.messages

    suspend fun create(item: MessageItem): Either<RepositoryError, MessageItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findByThreadId(threadId: ObjectId): Either<RepositoryError, List<MessageItem>> =
        mongoCatch {
            collection
                .find(Filters.eq(MessageItem::threadId, threadId))
                .sort(Sorts.ascending(MessageItem::sentAt))
                .toList()
        }
}
