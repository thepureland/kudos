package io.kudos.ms.tag.common.rule.model

import kotlinx.serialization.Serializable

/** Operators supported by a published attribute rule. */
@Serializable
enum class TagRuleOperator {
    EQ,
    NE,
    GT,
    GTE,
    LT,
    LTE,
    BETWEEN,
    IN,
    NOT_IN,
    CONTAINS,
    EXISTS,
    NOT_EXISTS,
}
