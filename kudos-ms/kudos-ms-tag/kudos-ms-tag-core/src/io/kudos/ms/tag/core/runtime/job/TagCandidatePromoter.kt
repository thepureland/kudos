package io.kudos.ms.tag.core.runtime.job

import io.kudos.ms.tag.core.assignment.service.iservice.ITagAssignmentService
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentTransactionExecutor
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.model.TagRuleStatus
import io.kudos.ms.tag.core.rule.service.impl.TagRuleService
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationCandidateDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

data class CandidatePromotionResult(
    val jobId: String,
    val subjectsProcessed: Int,
    val matchedSubjects: Int,
    val assignmentChanges: Int,
)

@Component
open class TagCandidatePromoter(
    private val tagDao: TagDefinitionDao,
    private val ruleDao: TagRuleDao,
    private val ruleService: TagRuleService,
    private val subjectDao: TagSubjectDao,
    private val candidateDao: TagRecalculationCandidateDao,
    private val assignmentService: ITagAssignmentService,
    private val jobDao: TagRecalculationJobDao,
    private val transactionExecutor: TagAssignmentTransactionExecutor,
) {
    open fun promote(jobId: String): CandidatePromotionResult = transactionExecutor.execute {
        val job = requireNotNull(jobDao.get(jobId)) { "Recalculation job [$jobId] does not exist." }
        require(job.jobType == "RULE_FULL_REBUILD" && job.status == "RUNNING") {
            "Only an active full-rebuild job may be promoted."
        }
        require(subjectDao.listKeysAfter(job.tenantId, job.subjectType, job.cursorSubjectId, 1).isEmpty()) {
            "Full-rebuild subject scan is not exhausted."
        }
        val tag = tagDao.get(job.tagId)
            ?.takeIf { it.tenantId == job.tenantId && it.subjectType == job.subjectType }
            ?: throw IllegalArgumentException("Full-rebuild tag [${job.tagId}] is outside the job scope.")
        val rule = ruleDao.listByTag(job.tenantId, tag.id)
            .singleOrNull { it.ruleVersion == job.ruleVersion }
            ?: throw IllegalArgumentException("Full-rebuild rule version [${job.ruleVersion}] does not exist.")
        require(rule.status == TagRuleStatus.REBUILDING.name) { "Full-rebuild rule is no longer REBUILDING." }
        val matchedIds = candidateDao.listByRun(job.id).also { candidates ->
            require(candidates.all {
                it.tenantId == job.tenantId && it.subjectType == job.subjectType &&
                    it.tagId == tag.id && it.ruleVersion == rule.ruleVersion
            }) { "Full-rebuild candidates do not match the job scope." }
        }.mapTo(hashSetOf()) { it.subjectId }

        var after: String? = null
        var subjects = 0
        var changes = 0
        while (true) {
            val page = subjectDao.listKeysAfter(job.tenantId, job.subjectType, after, 1000)
            if (page.isEmpty()) break
            page.forEach { key ->
                val delta = assignmentService.applyRuleResult(
                    key,
                    tag.id,
                    rule.id,
                    rule.ruleVersion,
                    key.subjectId in matchedIds,
                    "job:${job.id}",
                )
                changes += delta.assignedTagIds.size + delta.removedTagIds.size
                subjects += 1
            }
            after = page.last().subjectId
        }
        ruleService.completePublication(job.tenantId, rule.id)
        check(jobDao.completePromotion(job.id, LocalDateTime.now(ZoneOffset.UTC))) {
            "Full-rebuild job [${job.id}] could not be completed."
        }
        CandidatePromotionResult(job.id, subjects, matchedIds.size, changes)
    }
}
