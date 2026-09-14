package io.kudos.ms.tag.core.rule.engine

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.core.runtime.port.TagEvaluationContext
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator

/** Deterministic in-process evaluator over one immutable subject snapshot. */
open class JvmTagRuleEvaluator : TagRuleEvaluator {
    override fun evaluate(expression: TagRuleExpression, context: TagEvaluationContext): Boolean = when (expression) {
        is TagRuleExpression.AllOf -> expression.children.all { evaluate(it, context) }
        is TagRuleExpression.AnyOf -> expression.children.any { evaluate(it, context) }
        is TagRuleExpression.Not -> !evaluate(expression.child, context)
        is TagRuleExpression.HasTag -> expression.tagCode in context.activeTagCodes
        is TagRuleExpression.AttributePredicate -> evaluatePredicate(expression, context)
    }

    private fun evaluatePredicate(
        predicate: TagRuleExpression.AttributePredicate,
        context: TagEvaluationContext,
    ): Boolean {
        val values = context.attributes[predicate.attributeCode].orEmpty()
        return when (predicate.operator) {
            TagRuleOperator.EXISTS -> values.isNotEmpty()
            TagRuleOperator.NOT_EXISTS -> values.isEmpty()
            // Missing is unknown rather than "different", so every value comparison stays false.
            else -> values.isNotEmpty() && evaluatePresent(predicate, values, context)
        }
    }

    private fun evaluatePresent(
        predicate: TagRuleExpression.AttributePredicate,
        values: List<TagAttributeValue>,
        context: TagEvaluationContext,
    ): Boolean {
        val operands = predicate.operands
        return when (predicate.operator) {
            TagRuleOperator.EQ -> values.any { TagValueComparator.equal(it, operands.single()) }
            TagRuleOperator.NE -> values.none { TagValueComparator.equal(it, operands.single()) }
            TagRuleOperator.GT -> values.any { compare(it, operands.single()) { result -> result > 0 } }
            TagRuleOperator.GTE -> values.any { compare(it, operands.single()) { result -> result >= 0 } }
            TagRuleOperator.LT -> values.any { compare(it, operands.single()) { result -> result < 0 } }
            TagRuleOperator.LTE -> values.any { compare(it, operands.single()) { result -> result <= 0 } }
            TagRuleOperator.BETWEEN -> values.any { value ->
                compare(value, operands[0]) { it >= 0 } && compare(value, operands[1]) { it <= 0 }
            }
            TagRuleOperator.IN -> values.any { value -> operands.any { TagValueComparator.equal(value, it) } }
            TagRuleOperator.NOT_IN -> values.none { value -> operands.any { TagValueComparator.equal(value, it) } }
            TagRuleOperator.CONTAINS -> contains(predicate.attributeCode, values, operands.single(), context)
            TagRuleOperator.EXISTS, TagRuleOperator.NOT_EXISTS -> error("Presence operators are handled before value comparison.")
        }
    }

    private fun contains(
        attributeCode: String,
        values: List<TagAttributeValue>,
        operand: TagAttributeValue,
        context: TagEvaluationContext,
    ): Boolean = if (context.attributeCardinalities[attributeCode] == TagAttributeCardinality.MULTIPLE) {
        values.any { TagValueComparator.equal(it, operand) }
    } else {
        val needle = (operand as? TagAttributeValue.StringValue)?.value ?: return false
        values.any { (it as? TagAttributeValue.StringValue)?.value?.contains(needle) == true }
    }

    private inline fun compare(
        left: TagAttributeValue,
        right: TagAttributeValue,
        predicate: (Int) -> Boolean,
    ): Boolean = TagValueComparator.compare(left, right)?.let(predicate) ?: false
}
