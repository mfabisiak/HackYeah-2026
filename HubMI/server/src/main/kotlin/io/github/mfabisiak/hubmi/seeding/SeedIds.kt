package io.github.mfabisiak.hubmi.seeding

import org.bson.types.ObjectId
import java.security.MessageDigest

private const val OBJECT_ID_BYTES = 12

/**
 * The slug lives only in the seed files: it is hashed into a stable `_id`, so re-running the seeder upserts the same
 * documents without a technical key in the model (and tests can refer to a seeded document by its slug).
 */
fun seedObjectId(slug: String): ObjectId =
    ObjectId(MessageDigest.getInstance("SHA-1").digest(slug.toByteArray()).copyOf(OBJECT_ID_BYTES))
