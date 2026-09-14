package io.kudos.ms.tag.core.runtime.job

import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentTransactionExecutor
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.model.TagRuleStatus
import io.kudos.ms.tag.core.rule.service.iservice.ITagRuleService
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationCandidateDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationCandidate
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.port.TagEvaluationContext
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

data class CandidateBuildResult(
    val jobId: String,
    val processedSubjects: Int,
    val matchedSubjects: Int,
    val cursorSubjectId: String?,
    val exhausted: Boolean,
)

@Component
open class TagCandidateBuilder(
    private val tagDao: TagDefinitionDao,
    private val ruleDao: TagRuleDao,
    private val ruleService: ITagRuleService,
    private val subjectDao: TagSubjectDao,
    private val attributeDao: TagAttributeDefinitionDao,
    private val attributeStateStore: AttributeStateStore,
    private val assignmentDao: TagAssignmentDao,
    private val evaluator: TagRuleEvaluator,
    private val candidateDao: TagRecalculationCandidateDao,
    private val jobDao: TagRecalculationJobDao,
    private val transactionExecutor: TagAssignmentTransactionExecutor,
) {
    open fun build(job: LeasedRecalculationJob, batchSize: Int): CandidateBuildResult {
        require(job.jobType == RecalculationJobType.RULE_FULL_REBUILD) { "Candidate builder requires a full-rebuild job." }
        require(batchSize in 1..1000) { "Candidate build batch size must be between 1 and 1000." }
        return transactionExecutor.execute {
            val current = requireNotNull(jobDao.get(job.id)) { "Recalculation job [${job.id}] does not exist." }
            require(current.status == "RUNNING" && current.leaseOwner == job.leaseOwner) {
                "Recalculation job [${job.id}] is not leased by [${job.leaseOwner}]."
            }
            val tag = tagDao.get(job.tagId)
                ?.takeIf { it.tenantId == job.tenantId && it.subjectType == job.subjectType }
                ?: throw IllegalArgumentException("Full-rebuild tag [${job.tagId}] is outside the job scope.")
            val ruleRow = ruleDao.listByTag(job.tenantId, tag.id)
                .singleOrNull { it.ruleVersion == job.ruleVersion }
                ?: throw IllegalArgumentException("Rule version [${job.ruleVersion}] does not exist for tag [${tag.id}].")
            require(ruleRow.status == TagRuleStatus.REBUILDING.name) { "Candidate rule must be REBUILDING." }
            val rule = requireNotNull(ruleService.getVersion(job.tenantId, ruleRow.id, ruleRow.ruleVersion))
            val keys = subjectDao.listKeysAfter(job.tenantId, job.subjectType, current.cursorSubjectId, batchSize)
            val attributeCodes = rule.expression.attributeCodes()
            val cardinalities = attributeDao.listBySubjectType(job.tenantId, job.subjectType)
                .filter { it.code in attributeCodes }
                .associate { it.code to it.cardinality }
            var matches = 0
            val evaluatedTime = LocalDateTime.now(ZoneOffset.UTC)
            keys.forEach { key ->
                val activeTags = assignmentDao.list(key).mapNotNull { tagDao.get(it.tagId)?.code }.toSet()
                if (
                    evaluator.evaluate(
                        rule.expression,
                        TagEvaluationContext(
                            key,
                            attributeStateStore.load(key, attributeCodes),
                            cardinalities,
                            activeTags,
                        ),
                    )
                ) {
                    candidateDao.insertCandidate(TagRecalculationCandidate().apply {
                        runId = job.id
                        tenantId = job.tenantId
                        subjectType = job.subjectType
                        subjectId = key.subjectId
                        tagId = tag.id
                        ruleVersion = rule.ruleVersion
                        this.evaluatedTime = evaluatedTime
                    })
                    matches += 1
                }
            }
            val exhausted = keys.size < batchSize
            val cursor = keys.lastOrNull()?.subjectId ?: current.cursorSubjectId
            check(
                jobDao.advanceFullRebuild(
                    job.id,
                    job.leaseOwner,
                    cursor,
                    keys.size.toLong(),
                    exhausted,
                    evaluatedTime,
                )
            ) { "Recalculation job [${job.id}] lost its lease while recording candidate progress." }
            CandidateBuildResult(job.id, keys.size, matches, cursor, exhausted)
        }
    }

    private fun TagRuleExpression.attributeCodes(): Set<String> = when (this) {
        is TagRuleExpression.AllOf -> children.flatMap { it.attributeCodes() }.toSet()
        is TagRuleExpression.AnyOf -> children.flatMap { it.attributeCodes() }.toSet()
        is TagRuleExpression.Not -> child.attributeCodes()
        is TagRuleExpression.AttributePredicate -> setOf(attributeCode)
        is TagRuleExpression.HasTag -> emptySet()
    }
}
