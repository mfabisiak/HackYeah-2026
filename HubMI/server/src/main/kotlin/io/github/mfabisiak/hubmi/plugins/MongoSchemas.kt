package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.ValidationAction
import com.mongodb.client.model.ValidationLevel
import com.mongodb.client.model.ValidationOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.path
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.RepositoryError
import io.github.mfabisiak.hubmi.repository.SAMPLES_COLLECTION
import io.github.mfabisiak.hubmi.repository.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.Document

object MongoSchemas {
    private val sampleValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to listOf(SampleItem::slug.path(), SampleItem::name.path()),
                    "properties" to
                        Document(
                            mapOf(
                                SampleItem::slug.path() to Document("bsonType", "string"),
                                SampleItem::name.path() to Document("bsonType", "string"),
                            ),
                        ),
                ),
            ),
        )

    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            val existingCollections = database.listCollectionNames().toList().toSet()

            if (SAMPLES_COLLECTION !in existingCollections) {
                database.createCollection(
                    SAMPLES_COLLECTION,
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
                            "collMod" to SAMPLES_COLLECTION,
                            "validator" to sampleValidator,
                            "validationLevel" to ValidationLevel.MODERATE.value,
                            "validationAction" to ValidationAction.ERROR.value,
                        ),
                    ),
                )
            }
        }.map { }
}
