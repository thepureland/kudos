package io.kudos.ms.tag.core.rule.model

import io.kudos.ms.tag.core.rule.model.po.TagRule
import io.kudos.ms.tag.core.rule.model.po.TagRuleDependency
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand

data class PersistedRuleTree(
    val rule: TagRule,
    val nodes: List<TagRuleNode>,
    val operands: List<TagRuleOperand>,
    val dependencies: List<TagRuleDependency>,
)
