package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.ValidationAction
import com.mongodb.client.model.ValidationLevel
import com.mongodb.client.model.ValidationOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.path
import io.github.mfabisiak.hubmi.models.ApplicationItem
import io.github.mfabisiak.hubmi.models.GrantCallItem
import io.github.mfabisiak.hubmi.models.IdeaItem
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.repository.APPLICATIONS_COLLECTION
import io.github.mfabisiak.hubmi.repository.GRANT_CALLS_COLLECTION
import io.github.mfabisiak.hubmi.repository.IDEAS_COLLECTION
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

    private val ideaValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to
                        listOf(
                            IdeaItem::authorId.path(),
                            IdeaItem::title.path(),
                            IdeaItem::essence.path(),
                            IdeaItem::status.path(),
                        ),
                    "properties" to
                        Document(
                            mapOf(
                                IdeaItem::authorId.path() to Document("bsonType", "string"),
                                IdeaItem::title.path() to Document("bsonType", "string"),
                                IdeaItem::essence.path() to Document("bsonType", "string"),
                                IdeaItem::status.path() to Document("bsonType", "string"),
                            ),
                        ),
                ),
            ),
        )

    private val grantCallValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to
                        listOf(
                            GrantCallItem::title.path(),
                            GrantCallItem::description.path(),
                            GrantCallItem::opensAt.path(),
                            GrantCallItem::closesAt.path(),
                        ),
                    "properties" to
                        Document(
                            mapOf(
                                GrantCallItem::title.path() to Document("bsonType", "string"),
                                GrantCallItem::description.path() to Document("bsonType", "string"),
                                GrantCallItem::opensAt.path() to Document("bsonType", "date"),
                                GrantCallItem::closesAt.path() to Document("bsonType", "date"),
                            ),
                        ),
                ),
            ),
        )

    private val applicationValidator =
        Document(
            "\$jsonSchema",
            Document(
                mapOf(
                    "bsonType" to "object",
                    "required" to
                        listOf(
                            ApplicationItem::callId.path(),
                            ApplicationItem::applicantId.path(),
                            ApplicationItem::answers.path(),
                        ),
                    "properties" to
                        Document(
                            mapOf(
                                ApplicationItem::callId.path() to Document("bsonType", "objectId"),
                                ApplicationItem::applicantId.path() to Document("bsonType", "string"),
                                ApplicationItem::answers.path() to Document("bsonType", "object"),
                            ),
                        ),
                ),
            ),
        )

    suspend fun configure(database: MongoDatabase): Either<RepositoryError, Unit> =
        mongoCatch {
            val existingCollections = database.listCollectionNames().toList().toSet()

            ensureCollectionWithSchema(database, existingCollections, SAMPLES_COLLECTION, sampleValidator)
            ensureCollectionWithSchema(database, existingCollections, IDEAS_COLLECTION, ideaValidator)
            ensureCollectionWithSchema(database, existingCollections, GRANT_CALLS_COLLECTION, grantCallValidator)
            ensureCollectionWithSchema(database, existingCollections, APPLICATIONS_COLLECTION, applicationValidator)
        }.map { }

    private suspend fun ensureCollectionWithSchema(
        database: MongoDatabase,
        existingCollections: Set<String>,
        collectionName: String,
        validator: Document,
    ) {
        if (collectionName !in existingCollections) {
            database.createCollection(
                collectionName,
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
                        "collMod" to collectionName,
                        "validator" to validator,
                        "validationLevel" to ValidationLevel.MODERATE.value,
                        "validationAction" to ValidationAction.ERROR.value,
                    ),
                ),
            )
        }
    }
}
