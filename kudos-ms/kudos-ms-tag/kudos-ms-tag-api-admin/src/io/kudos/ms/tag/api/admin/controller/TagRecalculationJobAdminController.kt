package io.kudos.ms.tag.api.admin.controller

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.RecalculationRequest
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

data class TagRecalculationJobView(
    val id: String,
    val tenantId: String,
    val jobType: String,
    val tagId: String,
    val ruleVersion: Long,
    val subjectType: String,
    val subjectId: String?,
    val status: String,
    val attemptCount: Int,
    val processedCount: Long,
    val lastErrorCode: String?,
    val updateTime: LocalDateTime,
)

@RestController
@RequestMapping("/api/admin/tag/recalculation-job")
open class TagRecalculationJobAdminController(
    private val jobDao: TagRecalculationJobDao,
    private val queue: RecalculationQueue,
    private val tenantAccessGuard: TagTenantAccessGuard,
) {
    @GetMapping
    @RequiresPermission("tag:job:view")
    open fun list(@RequestParam tenantId: String): List<TagRecalculationJobView> {
        tenantAccessGuard.requireTenant(tenantId)
        return jobDao.listByTenant(tenantId).map {
            TagRecalculationJobView(
                it.id, it.tenantId, it.jobType, it.tagId, it.ruleVersion, it.subjectType, it.subjectId,
                it.status, it.attemptCount, it.processedCount, it.lastErrorCode, it.updateTime,
            )
        }
    }

    @PostMapping("/retry")
    @RequiresPermission("tag:job:retry")
    open fun retry(@RequestParam id: String): String {
        val job = requireNotNull(jobDao.get(id)) { "Recalculation job [$id] does not exist." }
        tenantAccessGuard.requireTenant(job.tenantId)
        return queue.request(
            RecalculationRequest(
                tenantId = job.tenantId,
                jobType = RecalculationJobType.valueOf(job.jobType),
                tagId = job.tagId,
                ruleVersion = job.ruleVersion,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                priority = job.priority,
                requestedVersion = job.requestedVersion + 1,
            )
        )
    }

    @PostMapping("/cancel")
    @RequiresPermission("tag:job:cancel")
    open fun cancel(@RequestParam id: String): Boolean {
        val job = requireNotNull(jobDao.get(id)) { "Recalculation job [$id] does not exist." }
        tenantAccessGuard.requireTenant(job.tenantId)
        return queue.cancel(id)
    }
}
