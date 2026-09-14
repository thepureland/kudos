package io.kudos.ms.tag.core.runtime.port

import java.time.Instant

interface RecalculationQueue {
    fun request(command: RecalculationRequest): String
    fun lease(workerId: String, limit: Int, leaseUntil: Instant): List<LeasedRecalculationJob>
}
