package io.kudos.ms.tag.core.runtime.job

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Passive until the embedding application enables Spring scheduling and this worker property. */
@Component
open class TagRecalculationScheduler(
    private val worker: TagRecalculationWorker,
    private val properties: TagRecalculationProperties,
    @Value("\${spring.application.name:kudos-tag}-\${random.uuid}") private val workerId: String,
) {
    @Scheduled(fixedDelayString = "\${kudos.tag.recalculation.poll-delay:5s}")
    open fun poll() {
        if (properties.schedulingEnabled) worker.runOnce(workerId)
    }
}
