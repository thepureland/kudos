package io.kudos.ms.tag.core.runtime.service.iservice

import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob

data class RecalculationSummary(
    val subjectsProcessed: Int,
    val directRulesEvaluated: Int,
    val cascadedRulesEvaluated: Int,
    val assignmentChanges: Int,
)

interface ITagRecalculationService {
    fun requestIncremental(keys: Collection<TagSubjectKey>, reason: String): List<String>
    fun recalculateSubjectNow(keys: Collection<TagSubjectKey>, directRuleLimit: Int = 200): RecalculationSummary
    fun process(job: LeasedRecalculationJob): RecalculationSummary
}
