package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.ErrorCategory
import com.mongodb.MongoBulkWriteException
import com.mongodb.MongoCommandException
import com.mongodb.MongoWriteException
import com.mongodb.kotlin.client.coroutine.ClientSession

fun Throwable.isDuplicateKey(): Boolean =
    when (this) {
        is MongoWriteException -> {
            error.category == ErrorCategory.DUPLICATE_KEY || error.code == 11000
        }

        is MongoBulkWriteException -> {
            writeErrors.any {
                it.category == ErrorCategory.DUPLICATE_KEY || it.code == 11000
            }
        }

        is MongoCommandException -> {
            errorCode == 11000
        }

        else -> {
            message?.contains("E11000", ignoreCase = true) == true
        }
    }

suspend fun <T> mongoCatch(block: suspend () -> T): Either<RepositoryError, T> =
    Either
        .catch { block() }
        .mapLeft { throwable ->
            if (throwable.isDuplicateKey()) {
                RepositoryError.Conflict(throwable)
            } else {
                RepositoryError.DatabaseException(throwable)
            }
        }

suspend fun <T> MongoRepository.withTransaction(block: suspend (ClientSession) -> T): Either<RepositoryError, T> =
    mongoCatch {
        val session = client.startSession()
        try {
            session.startTransaction()
            val result = block(session)
            session.commitTransaction()
            result
        } catch (t: Throwable) {
            session.abortTransaction()
            throw t
        } finally {
            session.close()
        }
    }
