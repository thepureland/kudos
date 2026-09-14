package io.kudos.ms.tag.core.runtime.job

import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import org.springframework.stereotype.Service

data class FullRecalculationBatchResult(
    val build: CandidateBuildResult,
    val promotion: CandidatePromotionResult?,
)

@Service
open class FullRecalculationService(
    private val builder: TagCandidateBuilder,
    private val promoter: TagCandidatePromoter,
) {
    open fun process(job: LeasedRecalculationJob, batchSize: Int): FullRecalculationBatchResult {
        val build = builder.build(job, batchSize)
        val promotion = if (build.exhausted) promoter.promote(job.id) else null
        return FullRecalculationBatchResult(build, promotion)
    }
}
