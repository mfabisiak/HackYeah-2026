package io.github.mfabisiak.hubmi.repository

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.models.SampleItem

/**
 * Single place binding a collection name to its document type. The driver encodes `@Serializable` models with
 * bson-kotlinx (enums by name, `ObjectId` via `@Contextual`), so repositories never touch untyped `Document`s.
 */
const val SAMPLES_COLLECTION = "samples"

val MongoDatabase.samples: MongoCollection<SampleItem>
    get() = getCollection<SampleItem>(SAMPLES_COLLECTION)
