package io.kudos.ms.tag.core.runtime.port

import java.time.Instant

interface RecalculationQueue {
    fun request(command: RecalculationRequest): String
    fun lease(workerId: String, limit: Int, leaseUntil: Instant): List<LeasedRecalculationJob>
    fun complete(jobId: String, workerId: String, leaseVersion: Long, processedVersion: Long, processedCount: Long): Boolean
    fun fail(jobId: String, workerId: String, leaseVersion: Long, errorCode: String, errorMessage: String?): Boolean
    fun cancel(jobId: String): Boolean
}
