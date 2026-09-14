package io.kudos.ms.tag.core.rule.engine

import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.core.runtime.port.TagEvaluationContext
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator

/** Default in-process evaluator boundary; exact typed semantics are implemented in Task 9. */
open class JvmTagRuleEvaluator : TagRuleEvaluator {
    override fun evaluate(expression: TagRuleExpression, context: TagEvaluationContext): Boolean =
        error("JVM tag rule evaluation is not initialized yet.")
}
