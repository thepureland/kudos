package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.catalog.model.TagSubjectTypeView
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/tag/subject-type")
open class TagSubjectTypeAdminController(
    private val catalogService: ITagCatalogService,
    private val subjectTypeDao: TagSubjectTypeDao,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping
    @RequiresPermission("tag:subject-type:view")
    open fun get(@RequestParam code: String): TagSubjectTypeView {
        val subjectType = requireNotNull(subjectTypeDao.findByCode(code)) { "Subject type [$code] does not exist." }
        return TagSubjectTypeView(subjectType.id, subjectType.code, subjectType.name, subjectType.ownerServiceCode)
    }

    @PostMapping
    @RequiresPermission("tag:subject-type:view")
    open fun register(@RequestBody command: RegisterTagSubjectTypeCommand): TagSubjectTypeView {
        tenantAccessGuard.requirePlatformManagement()
        return catalogService.registerSubjectType(command)
    }
}
