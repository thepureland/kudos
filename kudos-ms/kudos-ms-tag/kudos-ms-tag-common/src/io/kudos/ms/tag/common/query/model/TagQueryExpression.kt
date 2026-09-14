package io.kudos.ms.tag.common.query.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.requireTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Boolean expression over materialized tags only; attribute predicates are intentionally absent. */
@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(TagQueryExpression.AllTags::class, name = "allTags"),
    JsonSubTypes.Type(TagQueryExpression.AnyTags::class, name = "anyTags"),
    JsonSubTypes.Type(TagQueryExpression.NotTags::class, name = "notTags"),
)
sealed interface TagQueryExpression {

    @Serializable
    @SerialName("allTags")
    data class AllTags(
        val tagCodes: Set<String> = emptySet(),
        val nested: List<TagQueryExpression> = emptyList(),
    ) : TagQueryExpression {
        init {
            validateGroup("AllTags", tagCodes, nested)
        }
    }

    @Serializable
    @SerialName("anyTags")
    data class AnyTags(
        val tagCodes: Set<String> = emptySet(),
        val nested: List<TagQueryExpression> = emptyList(),
    ) : TagQueryExpression {
        init {
            validateGroup("AnyTags", tagCodes, nested)
        }
    }

    @Serializable
    @SerialName("notTags")
    data class NotTags(val child: TagQueryExpression) : TagQueryExpression

    companion object {
        private val CODE_PATTERN = Regex("^[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)*$")

        private fun validateGroup(
            name: String,
            tagCodes: Set<String>,
            nested: List<TagQueryExpression>,
        ) {
            requireTag(tagCodes.isNotEmpty() || nested.isNotEmpty(), TagErrorCode.EMPTY_QUERY_GROUP) {
                "$name must contain at least one tag code or nested expression"
            }
            requireTag(tagCodes.all(CODE_PATTERN::matches), TagErrorCode.INVALID_TAG_CODE) {
                "$name contains an invalid tag code"
            }
        }
    }
}
