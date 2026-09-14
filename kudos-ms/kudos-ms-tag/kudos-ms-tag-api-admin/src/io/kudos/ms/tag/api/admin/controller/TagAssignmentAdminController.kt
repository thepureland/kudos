package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentResponse
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.api.TagAssignmentApi
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tag/assignment")
open class TagAssignmentAdminController(
    private val assignmentApi: TagAssignmentApi,
    private val assignmentDao: TagAssignmentDao,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping
    @RequiresPermission("tag:assignment:view")
    open fun listTagIds(
        @RequestParam tenantId: String,
        @RequestParam subjectType: String,
        @RequestParam subjectId: String,
    ): List<String> {
        tenantAccessGuard.requireTenant(tenantId)
        return assignmentDao.list(TagSubjectKey(tenantId, subjectType, subjectId)).map { it.tagId }
    }

    @PostMapping("/manual")
    @RequiresPermission("tag:assignment:manual")
    open fun assignManual(@RequestBody request: AssignManualTagRequest): ManualTagAssignmentResponse =
        assignmentApi.assignManual(request)

    @DeleteMapping("/manual")
    @RequiresPermission("tag:assignment:manual")
    open fun removeManual(@RequestBody request: RemoveManualTagRequest): ManualTagAssignmentResponse =
        assignmentApi.removeManual(request)
}
