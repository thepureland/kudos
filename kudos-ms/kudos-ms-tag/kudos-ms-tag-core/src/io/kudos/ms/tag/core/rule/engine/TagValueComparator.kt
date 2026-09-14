package io.kudos.ms.tag.core.rule.engine

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue

/** Exact, type-preserving comparison used by every in-process rule operator. */
internal object TagValueComparator {

    fun equal(left: TagAttributeValue, right: TagAttributeValue): Boolean =
        compare(left, right) == 0

    /** Returns null when the two values do not belong to one comparable type domain. */
    fun compare(left: TagAttributeValue, right: TagAttributeValue): Int? = when {
        left is TagAttributeValue.StringValue && right is TagAttributeValue.StringValue ->
            left.value.compareTo(right.value)
        left is TagAttributeValue.IntegerValue && right is TagAttributeValue.IntegerValue ->
            left.value.compareTo(right.value)
        left is TagAttributeValue.DecimalValue && right is TagAttributeValue.DecimalValue ->
            left.toBigDecimal().compareTo(right.toBigDecimal())
        left is TagAttributeValue.BooleanValue && right is TagAttributeValue.BooleanValue ->
            left.value.compareTo(right.value)
        left is TagAttributeValue.DateValue && right is TagAttributeValue.DateValue ->
            left.toLocalDate().compareTo(right.toLocalDate())
        left is TagAttributeValue.DateTimeValue && right is TagAttributeValue.DateTimeValue ->
            left.toInstant().compareTo(right.toInstant())
        else -> null
    }
}
