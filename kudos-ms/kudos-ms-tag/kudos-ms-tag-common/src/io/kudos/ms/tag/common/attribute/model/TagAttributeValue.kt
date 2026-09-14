package io.kudos.ms.tag.common.attribute.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.requireTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** A typed attribute value with an unambiguous wire representation. */
@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(TagAttributeValue.StringValue::class, name = "string"),
    JsonSubTypes.Type(TagAttributeValue.IntegerValue::class, name = "integer"),
    JsonSubTypes.Type(TagAttributeValue.DecimalValue::class, name = "decimal"),
    JsonSubTypes.Type(TagAttributeValue.BooleanValue::class, name = "boolean"),
    JsonSubTypes.Type(TagAttributeValue.DateValue::class, name = "date"),
    JsonSubTypes.Type(TagAttributeValue.DateTimeValue::class, name = "datetime"),
)
sealed interface TagAttributeValue {

    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : TagAttributeValue

    @Serializable
    @SerialName("integer")
    data class IntegerValue(val value: Long) : TagAttributeValue

    @Serializable
    @SerialName("decimal")
    data class DecimalValue(val value: String) : TagAttributeValue {
        init {
            requireTag(DECIMAL_PATTERN.matches(value), TagErrorCode.INVALID_ATTRIBUTE_VALUE) {
                "decimal value must use a plain base-10 representation"
            }
            requireTag(runCatching { BigDecimal(value) }.isSuccess, TagErrorCode.INVALID_ATTRIBUTE_VALUE) {
                "decimal value is outside the supported range"
            }
        }

        fun toBigDecimal(): BigDecimal = BigDecimal(value)
    }

    @Serializable
    @SerialName("boolean")
    data class BooleanValue(val value: Boolean) : TagAttributeValue

    @Serializable
    @SerialName("date")
    data class DateValue(val value: String) : TagAttributeValue {
        init {
            requireTag(runCatching { LocalDate.parse(value) }.isSuccess, TagErrorCode.INVALID_ATTRIBUTE_VALUE) {
                "date value must be an ISO-8601 local date"
            }
        }

        fun toLocalDate(): LocalDate = LocalDate.parse(value)
    }

    @Serializable
    @SerialName("datetime")
    data class DateTimeValue(val value: String) : TagAttributeValue {
        init {
            requireTag(runCatching { OffsetDateTime.parse(value) }.isSuccess, TagErrorCode.INVALID_ATTRIBUTE_VALUE) {
                "datetime value must be an ISO-8601 offset datetime"
            }
        }

        fun toInstant(): Instant = OffsetDateTime.parse(value).toInstant()
    }

    companion object {
        private val DECIMAL_PATTERN = Regex("^-?(0|[1-9][0-9]*)(\\.[0-9]+)?$")
    }
}
