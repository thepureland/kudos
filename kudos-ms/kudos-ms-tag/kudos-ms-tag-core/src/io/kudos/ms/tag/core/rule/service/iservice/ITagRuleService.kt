package io.kudos.ms.tag.core.rule.service.iservice

import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.core.rule.model.TagRulePublication
import io.kudos.ms.tag.core.rule.model.TagRuleView

interface ITagRuleService {
    fun createDraft(tenantId: String, tagCode: String, expression: TagRuleExpression): TagRuleView
    fun replaceDraft(tenantId: String, ruleId: String, expression: TagRuleExpression): TagRuleView
    fun requestPublication(tenantId: String, ruleId: String): TagRulePublication
    fun getPublished(tenantId: String, tagCode: String): TagRuleView?
    fun getVersion(tenantId: String, ruleId: String, ruleVersion: Long): TagRuleView?
}
