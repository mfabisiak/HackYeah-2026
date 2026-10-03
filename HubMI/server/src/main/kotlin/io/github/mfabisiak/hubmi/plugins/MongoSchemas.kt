package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.ValidationAction
import com.mongodb.client.model.ValidationLevel
import com.mongodb.client.model.ValidationOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.path
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.CHALLENGES_COLLECTION
import io.github.mfabisiak.hubmi.repository.INNOVATIONS_COLLECTION
import io.github.mfabisiak.hubmi.repository.MATERIALS_COLLECTION
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

    private val innovationValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to listOf("title", "summary", "description", "areas", "targetGroups", "stage"),
                    "properties" to
                        Document(
                            mapOf(
                                "title" to Document("bsonType", "string"),
                                "summary" to Document("bsonType", "string"),
                                "description" to Document("bsonType", "string"),
                            ),
                        ),
                ),
            ),
        )

    private val challengeValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to listOf("title", "description", "area"),
                    "properties" to
                        Document(
                            mapOf(
                                "title" to Document("bsonType", "string"),
                                "description" to Document("bsonType", "string"),
                            ),
                        ),
                ),
            ),
        )

    private val materialValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to listOf("title", "description", "type", "url"),
                    "properties" to
                        Document(
                            mapOf(
                                "title" to Document("bsonType", "string"),
                                "description" to Document("bsonType", "string"),
                                "url" to Document("bsonType", "string"),
                            ),
                        ),
                ),
            ),
        )

    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            val existingCollections = database.listCollectionNames().toList().toSet()
            ensureCollection(database, SAMPLES_COLLECTION, sampleValidator, existingCollections)
            ensureCollection(database, INNOVATIONS_COLLECTION, innovationValidator, existingCollections)
            ensureCollection(database, CHALLENGES_COLLECTION, challengeValidator, existingCollections)
            ensureCollection(database, MATERIALS_COLLECTION, materialValidator, existingCollections)
        }.map { }

    private suspend fun ensureCollection(
        database: MongoDatabase,
        name: String,
        validator: Document,
        existing: Set<String>,
    ) {
        if (name !in existing) {
            database.createCollection(
                name,
                CreateCollectionOptions().validationOptions(
                    ValidationOptions()
                        .validator(validator)
                        .validationLevel(ValidationLevel.MODERATE)
                        .validationAction(ValidationAction.ERROR),
                ),
            )
        } else {
            database.runCommand<Document>(
                Document(
                    mapOf(
                        "collMod" to name,
                        "validator" to validator,
                        "validationLevel" to ValidationLevel.MODERATE.value,
                        "validationAction" to ValidationAction.ERROR.value,
                    ),
                ),
            )
        }
    }
}
