package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.ValidationAction
import com.mongodb.client.model.ValidationLevel
import com.mongodb.client.model.ValidationOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.Document

object MongoSchemas {
    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            val existingCollections = database.listCollectionNames().toList().toSet()

            val sampleValidator =
                Document(
                    "\$jsonSchema",
                    Document(
                        mapOf(
                            "bsonType" to "object",
                            "required" to listOf("slug", "name"),
                            "properties" to
                                Document(
                                    mapOf(
                                        "slug" to Document("bsonType", "string"),
                                        "name" to Document("bsonType", "string"),
                                    ),
                                ),
                        ),
                    ),
                )

            if ("samples" !in existingCollections) {
                database.createCollection(
                    "samples",
                    CreateCollectionOptions().validationOptions(
                        ValidationOptions()
                            .validator(sampleValidator)
                            .validationLevel(ValidationLevel.MODERATE)
                            .validationAction(ValidationAction.ERROR),
                    ),
                )
            } else {
                database.runCommand<Document>(
                    Document(
                        mapOf(
                            "collMod" to "samples",
                            "validator" to sampleValidator,
                            "validationLevel" to "moderate",
                            "validationAction" to "error",
                        ),
                    ),
                )
            }
        }
}
