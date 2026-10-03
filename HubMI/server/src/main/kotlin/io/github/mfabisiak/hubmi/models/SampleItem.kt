package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.SampleDto
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@JvmInline
value class SampleId(
    val value: String,
)

data class SampleItem(
    @BsonId
    val id: ObjectId = ObjectId(),
    val slug: String,
    val name: String,
    val description: String,
)

fun SampleItem.toDto(): SampleDto =
    SampleDto(
        id = id.toHexString(),
        slug = slug,
        name = name,
        description = description,
    )
