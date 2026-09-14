package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import io.kudos.ms.tag.common.catalog.model.UpdateTagCommand
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tag/tag")
open class TagDefinitionAdminController(
    private val catalogService: ITagCatalogService,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping
    @RequiresPermission("tag:definition:view")
    open fun list(@RequestParam tenantId: String, @RequestParam subjectType: String): List<TagDefinitionView> {
        tenantAccessGuard.requireTenant(tenantId)
        return catalogService.listTags(tenantId, subjectType)
    }

    @PostMapping
    @RequiresPermission("tag:definition:manage")
    open fun create(@RequestBody command: CreateTagCommand): TagDefinitionView {
        tenantAccessGuard.requireTenant(command.tenantId)
        return catalogService.createTag(command)
    }

    @PutMapping
    @RequiresPermission("tag:definition:manage")
    open fun update(@RequestBody command: UpdateTagCommand): TagDefinitionView {
        tenantAccessGuard.requireTenant(command.tenantId)
        return catalogService.updateTag(command)
    }
}
