package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.common.catalog.model.CreateTagSetCommand
import io.kudos.ms.tag.common.catalog.model.TagSetView
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tag/tag-set")
open class TagSetAdminController(
    private val catalogService: ITagCatalogService,
    private val tagSetDao: TagSetDao,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping
    @RequiresPermission("tag:definition:view")
    open fun list(@RequestParam tenantId: String, @RequestParam subjectType: String): List<TagSetView> {
        tenantAccessGuard.requireTenant(tenantId)
        return tagSetDao.listBySubjectType(tenantId, subjectType).map {
            TagSetView(it.id, it.tenantId, it.subjectType, it.code, it.name, it.cardinality, it.defaultTagId)
        }
    }

    @PostMapping
    @RequiresPermission("tag:definition:manage")
    open fun create(@RequestBody command: CreateTagSetCommand): TagSetView {
        tenantAccessGuard.requireTenant(command.tenantId)
        return catalogService.createTagSet(command)
    }

    @PostMapping("/default")
    @RequiresPermission("tag:definition:manage")
    open fun setDefault(
        @RequestParam tenantId: String,
        @RequestParam tagSetId: String,
        @RequestParam tagId: String,
    ): TagSetView {
        tenantAccessGuard.requireTenant(tenantId)
        return catalogService.setDefaultTag(tenantId, tagSetId, tagId)
    }
}
