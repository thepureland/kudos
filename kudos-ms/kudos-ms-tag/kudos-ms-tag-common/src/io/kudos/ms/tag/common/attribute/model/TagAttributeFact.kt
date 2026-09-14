package io.kudos.ms.tag.common.attribute.model

import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.requireTag
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/** Idempotent, tenant-scoped fact used to change one subject attribute. */
@Serializable
data class TagAttributeFact(
    val eventId: String,
    val subjectKey: TagSubjectKey,
    val attributeCode: String,
    val operation: TagAttributeOperation,
    val value: TagAttributeValue?,
    @Serializable(with = InstantAsIsoStringSerializer::class)
    val occurredAt: Instant,
    val sourceCode: String,
    val sourceVersion: Long? = null,
) {
    init {
        requireTag(eventId.isNotBlank(), TagErrorCode.INVALID_ATTRIBUTE_FACT) {
            "eventId must not be blank"
        }
        requireTag(CODE_PATTERN.matches(attributeCode), TagErrorCode.INVALID_ATTRIBUTE_CODE) {
            "attributeCode must be a stable lowercase code"
        }
        requireTag(sourceCode.isNotBlank(), TagErrorCode.INVALID_ATTRIBUTE_FACT) {
            "sourceCode must not be blank"
        }
        requireTag(sourceVersion == null || sourceVersion >= 0, TagErrorCode.INVALID_ATTRIBUTE_FACT) {
            "sourceVersion must not be negative"
        }
        requireTag(
            if (operation == TagAttributeOperation.CLEAR) value == null else value != null,
            TagErrorCode.INVALID_ATTRIBUTE_FACT,
        ) {
            "CLEAR requires no value and every other operation requires one value"
        }
    }

    companion object {
        private val CODE_PATTERN = Regex("^[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)*$")
    }
}

/** Serializes [Instant] as an ISO-8601 string instead of a platform-specific structure. */
object InstantAsIsoStringSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
