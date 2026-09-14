package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.RecalculationRequest
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.job.RecalculationRetryPolicy
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationJob
import java.time.Instant
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.springframework.transaction.annotation.Transactional

/** Durable coalescing queue with lease-owner and version fencing. */
@Transactional(rollbackFor = [Exception::class])
open class RdbRecalculationQueue(
    private val jobDao: TagRecalculationJobDao,
    private val retryPolicy: RecalculationRetryPolicy = RecalculationRetryPolicy(),
    private val clock: Clock = Clock.systemUTC(),
    private val leaseDialects: List<RecalculationLeaseDialect> = listOf(
        H2RecalculationLeaseDialect(), MySqlRecalculationLeaseDialect(), PostgreSqlRecalculationLeaseDialect(),
    ),
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
            check(jobDao.requestAgain(existing.id, command.requestedVersion, now())) {
                "Recalculation job [${existing.id}] could not be requested again."
            }
            return existing.id
        }

        val now = now()
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
        check(jobDao.insertJob(job)) { "Recalculation job [${job.id}] could not be created." }
        return job.id
    }

    override fun lease(workerId: String, limit: Int, leaseUntil: Instant): List<LeasedRecalculationJob> {
        require(workerId.isNotBlank()) { "Worker id must not be blank." }
        require(limit in 1..100) { "Lease batch size must be between 1 and 100." }
        val now = now()
        val until = LocalDateTime.ofInstant(leaseUntil, ZoneOffset.UTC)
        require(until > now) { "Lease expiry must be in the future." }
        jobDao.failExhaustedLeases(now)
        val product = jobDao.databaseProductName()
        val dialect = leaseDialects.firstOrNull { it.supports(product) }
            ?: error("Unsupported recalculation queue database [$product].")
        return jobDao.lease(dialect, workerId, now, until, limit).map { id -> requireNotNull(jobDao.get(id)).toLease() }
    }

    override fun complete(
        jobId: String, workerId: String, leaseVersion: Long, processedVersion: Long, processedCount: Long,
    ): Boolean {
        require(processedVersion > 0) { "Processed version must be positive." }
        require(processedCount >= 0) { "Processed count must be non-negative." }
        return jobDao.complete(jobId, workerId, leaseVersion, processedVersion, processedCount, now())
    }

    override fun fail(
        jobId: String, workerId: String, leaseVersion: Long, errorCode: String, errorMessage: String?,
    ): Boolean {
        require(errorCode.isNotBlank() && errorCode.length <= 64) { "Error code must contain 1 to 64 characters." }
        require(errorMessage == null || errorMessage.length <= 1000) { "Error message must not exceed 1000 characters." }
        val job = jobDao.get(jobId) ?: return false
        val now = now()
        return jobDao.fail(
            jobId, workerId, leaseVersion, errorCode, errorMessage, now,
            now.plus(retryPolicy.delayForAttempt(job.attemptCount)),
            job.attemptCount >= job.maxAttempts,
        )
    }

    override fun cancel(jobId: String): Boolean = jobDao.cancel(jobId, now())

    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)

    private fun TagRecalculationJob.toLease() = LeasedRecalculationJob(
        id, tenantId, io.kudos.ms.tag.core.runtime.port.RecalculationJobType.valueOf(jobType), tagId, ruleVersion,
        subjectType, subjectId, cursorSubjectId, requestedVersion, processedVersion, requireNotNull(leaseOwner),
        requireNotNull(leaseUntil).toInstant(ZoneOffset.UTC), version,
    )
}
