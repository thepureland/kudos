package io.kudos.ms.tag.core.runtime.service.impl

import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.assignment.service.iservice.ITagAssignmentService
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.model.TagRuleView
import io.kudos.ms.tag.core.rule.service.iservice.ITagRuleService
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.job.TagRecalculationProperties
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.RecalculationRequest
import io.kudos.ms.tag.core.runtime.port.TagEvaluationContext
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator
import io.kudos.ms.tag.core.runtime.service.iservice.ITagRecalculationService
import io.kudos.ms.tag.core.runtime.service.iservice.RecalculationSummary
import org.springframework.stereotype.Service

@Service
open class TagRecalculationService(
    private val tagDao: TagDefinitionDao,
    private val attributeDao: TagAttributeDefinitionDao,
    private val ruleDao: TagRuleDao,
    private val dependencyDao: TagRuleDependencyDao,
    private val ruleService: ITagRuleService,
    private val attributeStateStore: AttributeStateStore,
    private val assignmentDao: TagAssignmentDao,
    private val assignmentService: ITagAssignmentService,
    private val evaluator: TagRuleEvaluator,
    private val queue: RecalculationQueue,
    private val properties: TagRecalculationProperties = TagRecalculationProperties(),
) : ITagRecalculationService {

    override fun requestIncremental(keys: Collection<TagSubjectKey>, reason: String): List<String> {
        require(reason.isNotBlank()) { "Incremental recalculation reason must not be blank." }
        return keys.distinct().flatMap { key ->
            directRules(key.tenantId, key.subjectType).map { rule ->
                queue.request(
                    RecalculationRequest(
                        tenantId = key.tenantId,
                        jobType = RecalculationJobType.SUBJECT_INCREMENTAL,
                        tagId = rule.tagId,
                        ruleVersion = rule.ruleVersion,
                        subjectType = key.subjectType,
                        subjectId = key.subjectId,
                    )
                )
            }
        }
    }

    override fun recalculateSubjectNow(
        keys: Collection<TagSubjectKey>,
        directRuleLimit: Int,
    ): RecalculationSummary {
        require(keys.size <= properties.synchronousSubjectLimit) {
            "Synchronous recalculation accepts at most ${properties.synchronousSubjectLimit} subjects."
        }
        require(directRuleLimit in 1..properties.synchronousDirectRuleLimit) {
            "Synchronous direct-rule limit must be between 1 and ${properties.synchronousDirectRuleLimit}."
        }
        var summary = RecalculationSummary(0, 0, 0, 0)
        keys.distinct().forEach { key ->
            val direct = directRules(key.tenantId, key.subjectType)
            require(direct.size <= directRuleLimit) {
                "Subject [${key.subjectType}] has ${direct.size} direct rules, exceeding the limit of $directRuleLimit."
            }
            summary += evaluateDirectRules(key, direct, "sync:${key.subjectId}")
        }
        return summary
    }

    override fun process(job: LeasedRecalculationJob): RecalculationSummary {
        require(job.jobType == RecalculationJobType.SUBJECT_INCREMENTAL) {
            "Incremental recalculation cannot process ${job.jobType} jobs."
        }
        val subjectId = requireNotNull(job.subjectId) { "Incremental recalculation job must identify a subject." }
        val key = TagSubjectKey(job.tenantId, job.subjectType, subjectId)
        val tag = requireNotNull(tagDao.get(job.tagId)) { "Tag [${job.tagId}] does not exist." }
        require(tag.tenantId == job.tenantId && tag.subjectType == job.subjectType) {
            "Recalculation job tag is outside the subject scope."
        }
        val rule = publishedRule(tag)
        require(rule.ruleVersion == job.ruleVersion) {
            "Recalculation job targets stale rule version [${job.ruleVersion}]."
        }
        return evaluateDirectRules(key, listOf(rule), "job:${job.id}")
    }

    private fun evaluateDirectRules(
        key: TagSubjectKey,
        direct: List<TagRuleView>,
        causeRef: String,
    ): RecalculationSummary {
        val changedTagIds = linkedSetOf<String>()
        var changes = 0
        val orderedDirect = orderRules(direct)
        orderedDirect.forEach { rule ->
            val delta = evaluate(key, rule, causeRef)
            val count = delta.changeCount()
            if (count > 0) changedTagIds += rule.tagId
            changes += count
        }

        var cascaded = 0
        val directRuleIds = direct.mapTo(hashSetOf()) { it.id }
        downstreamRules(key.tenantId, changedTagIds).filterNot { it.id in directRuleIds }.forEach { rule ->
            val upstreamTagIds = dependencyDao.listByRule(rule.id)
                .filter { it.dependencyType == "TAG" }
                .mapNotNull { it.referencedTagId }
                .toSet()
            if (upstreamTagIds.none { it in changedTagIds }) return@forEach
            val delta = evaluate(key, rule, causeRef)
            cascaded += 1
            val count = delta.changeCount()
            if (count > 0) changedTagIds += rule.tagId
            changes += count
        }
        return RecalculationSummary(1, direct.size, cascaded, changes)
    }

    private fun evaluate(key: TagSubjectKey, rule: TagRuleView, causeRef: String): AssignmentDelta {
        val attributeCodes = rule.expression.attributeCodes()
        val cardinalities = attributeDao.listBySubjectType(key.tenantId, key.subjectType)
            .filter { it.code in attributeCodes }
            .associate { it.code to it.cardinality }
        val activeTagCodes = assignmentDao.list(key)
            .mapNotNull { assignment -> tagDao.get(assignment.tagId)?.code }
            .toSet()
        val matched = evaluator.evaluate(
            rule.expression,
            TagEvaluationContext(
                subjectKey = key,
                attributes = attributeStateStore.load(key, attributeCodes),
                attributeCardinalities = cardinalities,
                activeTagCodes = activeTagCodes,
            ),
        )
        return assignmentService.applyRuleResult(key, rule.tagId, rule.id, rule.ruleVersion, matched, causeRef)
    }

    private fun directRules(tenantId: String, subjectType: String): List<TagRuleView> =
        publishedRules(tenantId, subjectType).filter { rule ->
            val dependencies = dependencyDao.listByRule(rule.id)
            dependencies.any { it.dependencyType == "ATTRIBUTE" } || dependencies.none { it.dependencyType == "TAG" }
        }

    private fun downstreamRules(tenantId: String, changedTagIds: Set<String>): List<TagRuleView> {
        if (changedTagIds.isEmpty()) return emptyList()
        val publishedById = ruleDao.listNonRetired(tenantId)
            .filter { it.status == "PUBLISHED" }
            .associateBy { it.id }
        val dependencies = dependencyDao.listByTenant(tenantId).filter { it.dependencyType == "TAG" }
        val reachable = linkedSetOf<String>()
        val frontier = ArrayDeque(changedTagIds)
        while (frontier.isNotEmpty()) {
            val tagId = frontier.removeFirst()
            dependencies.filter { it.referencedTagId == tagId }.forEach { dependency ->
                val rule = publishedById[dependency.ruleId] ?: return@forEach
                if (reachable.add(rule.id)) frontier.addLast(rule.tagId)
            }
        }

        val ordered = mutableListOf<TagRuleView>()
        val remaining = reachable.toMutableSet()
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { ruleId ->
                dependencies.filter { it.ruleId == ruleId }.all { dependency ->
                    val upstreamRuleId = publishedById.values.firstOrNull { it.tagId == dependency.referencedTagId }?.id
                    upstreamRuleId == null || upstreamRuleId !in remaining
                }
            }.sorted()
            check(ready.isNotEmpty()) { "Published tag-rule dependencies contain a cycle." }
            ready.forEach { ruleId ->
                val row = requireNotNull(publishedById[ruleId])
                ordered += publishedRule(requireNotNull(tagDao.get(row.tagId)))
                remaining -= ruleId
            }
        }
        return ordered
    }

    private fun orderRules(rules: List<TagRuleView>): List<TagRuleView> {
        val byId = rules.associateBy { it.id }
        val byTagId = rules.associateBy { it.tagId }
        val remaining = byId.keys.toMutableSet()
        val ordered = mutableListOf<TagRuleView>()
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { ruleId ->
                dependencyDao.listByRule(ruleId)
                    .filter { it.dependencyType == "TAG" }
                    .mapNotNull { it.referencedTagId }
                    .all { referencedTagId -> byTagId[referencedTagId]?.id !in remaining }
            }.sorted()
            check(ready.isNotEmpty()) { "Published tag-rule dependencies contain a cycle." }
            ready.forEach { ruleId ->
                ordered += requireNotNull(byId[ruleId])
                remaining -= ruleId
            }
        }
        return ordered
    }

    private fun publishedRules(tenantId: String, subjectType: String): List<TagRuleView> =
        tagDao.listBySubjectType(tenantId, subjectType)
            .filter { it.active && it.publishedRuleId != null }
            .map(::publishedRule)

    private fun publishedRule(tag: TagDefinition): TagRuleView =
        requireNotNull(ruleService.getPublished(tag.tenantId, tag.code)) {
            "Published rule for tag [${tag.id}] is unavailable."
        }

    private fun TagRuleExpression.attributeCodes(): Set<String> = when (this) {
        is TagRuleExpression.AllOf -> children.flatMap { it.attributeCodes() }.toSet()
        is TagRuleExpression.AnyOf -> children.flatMap { it.attributeCodes() }.toSet()
        is TagRuleExpression.Not -> child.attributeCodes()
        is TagRuleExpression.AttributePredicate -> setOf(attributeCode)
        is TagRuleExpression.HasTag -> emptySet()
    }

    private fun AssignmentDelta.changeCount(): Int = assignedTagIds.size + removedTagIds.size

    private operator fun RecalculationSummary.plus(other: RecalculationSummary) = RecalculationSummary(
        subjectsProcessed + other.subjectsProcessed,
        directRulesEvaluated + other.directRulesEvaluated,
        cascadedRulesEvaluated + other.cascadedRulesEvaluated,
        assignmentChanges + other.assignmentChanges,
    )
}
