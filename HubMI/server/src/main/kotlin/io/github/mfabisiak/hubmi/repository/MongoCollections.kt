package io.github.mfabisiak.hubmi.repository

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.models.ChallengeItem
import io.github.mfabisiak.hubmi.models.InnovationItem
import io.github.mfabisiak.hubmi.models.MaterialItem
import io.github.mfabisiak.hubmi.models.SampleItem

/**
 * Single place binding a collection name to its document type. The driver encodes `@Serializable` models with
 * bson-kotlinx (enums by name, `ObjectId` via `@Contextual`), so repositories never touch untyped `Document`s.
 */
const val SAMPLES_COLLECTION = "samples"
const val INNOVATIONS_COLLECTION = "innovations"
const val CHALLENGES_COLLECTION = "challenges"
const val MATERIALS_COLLECTION = "materials"

val MongoDatabase.samples: MongoCollection<SampleItem>
    get() = getCollection<SampleItem>(SAMPLES_COLLECTION)

val MongoDatabase.innovations: MongoCollection<InnovationItem>
    get() = getCollection<InnovationItem>(INNOVATIONS_COLLECTION)

val MongoDatabase.challenges: MongoCollection<ChallengeItem>
    get() = getCollection<ChallengeItem>(CHALLENGES_COLLECTION)

val MongoDatabase.materials: MongoCollection<MaterialItem>
    get() = getCollection<MaterialItem>(MATERIALS_COLLECTION)
