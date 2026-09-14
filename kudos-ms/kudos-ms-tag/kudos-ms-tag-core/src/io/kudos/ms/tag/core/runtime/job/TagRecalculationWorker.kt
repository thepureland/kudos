package io.kudos.ms.tag.core.runtime.job

import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.service.iservice.ITagRecalculationService
import org.springframework.stereotype.Component
import java.time.Clock

@Component
open class TagRecalculationWorker(
    private val queue: RecalculationQueue,
    private val service: ITagRecalculationService,
    private val fullRecalculationService: FullRecalculationService,
    private val properties: TagRecalculationProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    open fun runOnce(workerId: String): Int {
        val jobs = queue.lease(workerId, properties.batchSize, clock.instant().plus(properties.leaseDuration))
        jobs.forEach { job ->
            try {
                when (job.jobType) {
                    io.kudos.ms.tag.core.runtime.port.RecalculationJobType.SUBJECT_INCREMENTAL -> {
                        val summary = service.process(job)
                        check(
                            queue.complete(
                                job.id,
                                workerId,
                                job.version,
                                job.requestedVersion,
                                summary.subjectsProcessed.toLong(),
                            )
                        ) { "Recalculation job [${job.id}] lost its lease before completion." }
                    }
                    io.kudos.ms.tag.core.runtime.port.RecalculationJobType.RULE_FULL_REBUILD ->
                        fullRecalculationService.process(job, properties.batchSize)
                }
            } catch (error: Exception) {
                queue.fail(
                    job.id,
                    workerId,
                    job.version,
                    error::class.simpleName ?: "RECALCULATION_FAILED",
                    error.message?.take(1000),
                )
            }
        }
        return jobs.size
    }
}
