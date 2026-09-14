package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.core.rule.model.TagRulePublication
import io.kudos.ms.tag.core.rule.model.TagRuleView
import io.kudos.ms.tag.core.rule.service.iservice.ITagRuleService
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.*

data class CreateTagRuleDraftRequest(val tenantId: String, val tagCode: String, val expression: TagRuleExpression)
data class ReplaceTagRuleDraftRequest(val tenantId: String, val ruleId: String, val expression: TagRuleExpression)

@RestController
@RequestMapping("/api/admin/tag/rule")
open class TagRuleAdminController(
    private val ruleService: ITagRuleService,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping("/published")
    @RequiresPermission("tag:rule:view")
    open fun published(@RequestParam tenantId: String, @RequestParam tagCode: String): TagRuleView? {
        tenantAccessGuard.requireTenant(tenantId)
        return ruleService.getPublished(tenantId, tagCode)
    }

    @PostMapping("/draft")
    @RequiresPermission("tag:rule:manage")
    open fun createDraft(@RequestBody request: CreateTagRuleDraftRequest): TagRuleView {
        tenantAccessGuard.requireTenant(request.tenantId)
        return ruleService.createDraft(request.tenantId, request.tagCode, request.expression)
    }

    @PutMapping("/draft")
    @RequiresPermission("tag:rule:manage")
    open fun replaceDraft(@RequestBody request: ReplaceTagRuleDraftRequest): TagRuleView {
        tenantAccessGuard.requireTenant(request.tenantId)
        return ruleService.replaceDraft(request.tenantId, request.ruleId, request.expression)
    }

    @PostMapping("/publish")
    @RequiresPermission("tag:rule:publish")
    open fun publish(@RequestParam tenantId: String, @RequestParam ruleId: String): TagRulePublication {
        tenantAccessGuard.requireTenant(tenantId)
        return ruleService.requestPublication(tenantId, ruleId)
    }
}
