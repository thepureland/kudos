package io.kudos.ms.tag.core.rule.model

import io.kudos.ms.tag.common.rule.model.TagRuleExpression

enum class TagRuleStatus {
    DRAFT,
    VALIDATING,
    REBUILDING,
    PUBLISHED,
    RETIRED,
}

data class TagRuleView(
    val id: String,
    val tenantId: String,
    val tagId: String,
    val tagCode: String,
    val subjectType: String,
    val ruleVersion: Long,
    val status: TagRuleStatus,
    val expression: TagRuleExpression,
    val checksum: String,
)

/** Publication request handed to the replaceable rebuild runtime in Task 7. */
data class TagRulePublication(
    val rule: TagRuleView,
    val rebuildRequestKey: String,
)
