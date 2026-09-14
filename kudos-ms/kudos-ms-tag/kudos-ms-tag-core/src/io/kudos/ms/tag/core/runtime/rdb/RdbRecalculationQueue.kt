package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.RecalculationRequest
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationJob
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Default RDB adapter boundary; durable leasing is implemented in Task 12. */
open class RdbRecalculationQueue(
    private val jobDao: TagRecalculationJobDao,
) : RecalculationQueue {
    override fun request(command: RecalculationRequest): String {
        require(command.ruleVersion > 0) { "Recalculation rule version must be positive." }
        require(command.priority >= 0) { "Recalculation priority must be non-negative." }
        require(command.requestedVersion > 0) { "Requested recalculation version must be positive." }
        val key = listOf(
            command.jobType.name,
            command.tenantId,
            command.subjectType,
            command.subjectId,
            command.tagId,
            command.ruleVersion.toString(),
        ).joinToString("|") { value -> value?.let { "S${it.length}:$it" } ?: "N" }
        val existing = jobDao.findByJobKey(key)
        if (existing != null) {
            existing.requestedVersion = maxOf(existing.requestedVersion + 1, command.requestedVersion)
            existing.status = "PENDING"
            existing.availableTime = LocalDateTime.now(ZoneOffset.UTC)
            existing.updateTime = existing.availableTime
            existing.version += 1
            check(jobDao.update(existing)) { "Recalculation job [${existing.id}] could not be requested again." }
            return existing.id
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val job = TagRecalculationJob().apply {
            id = UUID.randomUUID().toString()
            jobKey = key
            tenantId = command.tenantId
            jobType = command.jobType.name
            tagId = command.tagId
            ruleVersion = command.ruleVersion
            subjectType = command.subjectType
            subjectId = command.subjectId
            cursorSubjectId = null
            status = "PENDING"
            priority = command.priority
            requestedVersion = command.requestedVersion
            processedVersion = 0
            attemptCount = 0
            maxAttempts = 10
            availableTime = now
            leaseOwner = null
            leaseUntil = null
            processedCount = 0
            lastErrorCode = null
            lastErrorMessage = null
            createTime = now
            startTime = null
            completeTime = null
            updateTime = now
            version = 0
        }
        jobDao.insert(job)
        return job.id
    }

    override fun lease(workerId: String, limit: Int, leaseUntil: Instant): List<LeasedRecalculationJob> =
        error("RDB recalculation leasing is not initialized yet.")
}
