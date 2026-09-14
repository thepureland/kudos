package io.kudos.ms.tag.common.rule.model

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.requireTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Versionable rule tree used by administrators to define a materialized segment. */
@Serializable
sealed interface TagRuleExpression {

    @Serializable
    @SerialName("all")
    data class AllOf(val children: List<TagRuleExpression>) : TagRuleExpression {
        init {
            requireTag(children.isNotEmpty(), TagErrorCode.EMPTY_RULE_GROUP) {
                "AllOf must contain at least one child"
            }
        }
    }

    @Serializable
    @SerialName("any")
    data class AnyOf(val children: List<TagRuleExpression>) : TagRuleExpression {
        init {
            requireTag(children.isNotEmpty(), TagErrorCode.EMPTY_RULE_GROUP) {
                "AnyOf must contain at least one child"
            }
        }
    }

    @Serializable
    @SerialName("not")
    data class Not(val child: TagRuleExpression) : TagRuleExpression

    @Serializable
    @SerialName("attribute")
    data class AttributePredicate(
        val attributeCode: String,
        val operator: TagRuleOperator,
        val operands: List<TagAttributeValue>,
    ) : TagRuleExpression {
        init {
            requireTag(CODE_PATTERN.matches(attributeCode), TagErrorCode.INVALID_ATTRIBUTE_CODE) {
                "attributeCode must be a stable lowercase code"
            }
            val validOperandCount = when (operator) {
                TagRuleOperator.EXISTS, TagRuleOperator.NOT_EXISTS -> operands.isEmpty()
                TagRuleOperator.BETWEEN -> operands.size == 2
                TagRuleOperator.IN, TagRuleOperator.NOT_IN -> operands.isNotEmpty()
                else -> operands.size == 1
            }
            requireTag(validOperandCount, TagErrorCode.INVALID_OPERAND_COUNT) {
                "operator $operator received ${operands.size} operands"
            }
        }
    }

    @Serializable
    @SerialName("hasTag")
    data class HasTag(val tagCode: String) : TagRuleExpression {
        init {
            requireTag(CODE_PATTERN.matches(tagCode), TagErrorCode.INVALID_TAG_CODE) {
                "tagCode must be a stable lowercase code"
            }
        }
    }

    companion object {
        private val CODE_PATTERN = Regex("^[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)*$")
    }
}
