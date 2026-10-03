package io.github.mfabisiak.hubmi.common.mongo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonDateTime
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
object JavaInstantAsBsonDateTime : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JavaInstantAsBsonDateTime", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        when (encoder) {
            is BsonEncoder -> {
                encoder.encodeBsonValue(
                    BsonDateTime(value.toEpochMilli()),
                )
            }

            else -> {
                encoder.encodeString(value.toString())
            }
        }
    }

    override fun deserialize(decoder: Decoder): Instant =
        when (decoder) {
            is BsonDecoder -> Instant.ofEpochMilli(decoder.decodeBsonValue().asDateTime().value)
            else -> Instant.parse(decoder.decodeString())
        }
}
