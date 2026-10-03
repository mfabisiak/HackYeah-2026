package io.github.mfabisiak.hubmi.common.mongo

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mongodb.ErrorCategory
import com.mongodb.MongoBulkWriteException
import com.mongodb.MongoCommandException
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.result.UpdateResult
import io.github.mfabisiak.hubmi.common.RepositoryError
import org.bson.conversions.Bson

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

suspend fun <T> catching(block: suspend () -> T): Either<RepositoryError, T> =
    Either
        .catch { block() }
        .mapLeft { throwable ->
            if (throwable.isDuplicateKey()) {
                RepositoryError.Conflict(throwable)
            } else {
                RepositoryError.DatabaseException(throwable)
            }
        }

suspend fun <T> mongoCatch(block: suspend () -> T): Either<RepositoryError, T> = catching(block)

/** Ensures that an update matched at least one document; otherwise returns [RepositoryError.Conflict]. */
fun UpdateResult.requireMatched(): Either<RepositoryError.Conflict, Unit> =
    if (matchedCount > 0) {
        Unit.right()
    } else {
        RepositoryError
            .Conflict(IllegalStateException("Warunek optymistycznej współbieżności nie został spełniony"))
            .left()
    }

/** For writes that must return a document (upserts): a missing one is a database fault, not a "not found". */
fun <T : Any> Either<RepositoryError, T?>.requireDocument(): Either<RepositoryError, T> =
    flatMap {
        it?.right()
            ?: RepositoryError.DatabaseException(IllegalStateException("Write returned no document")).left()
    }

/** `$and` rejects an empty list, so "no conditions" has to be spelled as the match-all filter. */
fun allOf(filters: List<Bson>): Bson = if (filters.isEmpty()) Filters.empty() else Filters.and(filters)
