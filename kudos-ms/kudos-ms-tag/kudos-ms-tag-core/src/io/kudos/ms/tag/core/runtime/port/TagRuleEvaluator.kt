package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.rule.model.TagRuleExpression

interface TagRuleEvaluator {
    fun evaluate(expression: TagRuleExpression, context: TagEvaluationContext): Boolean
}
