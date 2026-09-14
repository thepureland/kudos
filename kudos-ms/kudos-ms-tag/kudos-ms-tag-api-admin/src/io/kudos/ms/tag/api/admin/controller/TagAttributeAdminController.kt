package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.TagAttributeView
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tag/attribute")
open class TagAttributeAdminController(
    private val catalogService: ITagCatalogService,
    private val attributeDao: TagAttributeDefinitionDao,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping
    @RequiresPermission("tag:attribute:view")
    open fun list(@RequestParam tenantId: String, @RequestParam subjectType: String): List<TagAttributeView> {
        tenantAccessGuard.requireTenant(tenantId)
        return attributeDao.listBySubjectType(tenantId, subjectType).map {
            TagAttributeView(it.id, it.tenantId, it.subjectType, it.code, it.name, it.valueType, it.cardinality)
        }
    }

    @PostMapping
    @RequiresPermission("tag:attribute:manage")
    open fun create(@RequestBody command: CreateTagAttributeCommand): TagAttributeView {
        tenantAccessGuard.requireTenant(command.tenantId)
        return catalogService.createAttribute(command)
    }
}
