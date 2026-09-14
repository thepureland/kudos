package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import io.kudos.ms.tag.core.runtime.port.AssignmentSearchResult
import io.kudos.ms.tag.core.runtime.port.ResolvedAssignment
import io.kudos.ms.tag.core.runtime.port.TagAssignmentIndex
import io.kudos.ms.tag.core.runtime.port.TagKeysetPage
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignment
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Default RDB adapter for assignment materialization and tag-only subject queries. */
open class RdbTagAssignmentIndex(
    private val assignmentDao: TagAssignmentDao,
    private val assignmentEventDao: TagAssignmentEventDao,
    private val tagDao: TagDefinitionDao,
    private val tagSetDao: TagSetDao,
    private val queryCompiler: TagAssignmentQueryCompiler = TagAssignmentQueryCompiler(),
) : TagAssignmentIndex {
    override fun replaceForSet(
        key: TagSubjectKey,
        tagSetId: String?,
        winners: List<ResolvedAssignment>,
    ): AssignmentDelta {
        require(winners.all { it.tagSetId == tagSetId }) { "Every winner must belong to the set being replaced." }
        val cardinality = tagSetId?.let { requireNotNull(tagSetDao.get(it)).cardinality }
        val current = assignmentDao.list(key).filter { assignment ->
            tagDao.get(assignment.tagId)?.tagSetId == tagSetId
        }
        val oldIds = current.mapTo(linkedSetOf()) { it.tagId }
        val newIds = winners.mapTo(linkedSetOf()) { it.tagId }
        val assigned = newIds - oldIds
        val removed = oldIds - newIds
        val latestVersion = maxOf(
            assignmentDao.list(key).maxOfOrNull { it.assignmentVersion } ?: 0,
            assignmentEventDao.list(key).maxOfOrNull { it.assignmentVersion } ?: 0,
        )
        val version = if (assigned.isEmpty() && removed.isEmpty()) latestVersion else latestVersion + 1
        current.forEach { check(assignmentDao.deleteAssignment(key, it.tagId)) }
        val now = LocalDateTime.now(ZoneOffset.UTC)
        winners.forEach { winner ->
            check(assignmentDao.insertAssignment(TagAssignment().apply {
                tagId = winner.tagId
                tenantId = key.tenantId
                subjectType = key.subjectType
                subjectId = key.subjectId
                exclusiveSetId = winner.tagSetId.takeIf { cardinality == TagAttributeCardinality.SINGLE }
                assignmentVersion = version
                evaluatedRuleVersion = winner.evaluatedRuleVersion
                materializedTime = now
                effectiveFrom = winner.effectiveFrom?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
                effectiveUntil = winner.effectiveUntil?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
                updateTime = now
            }))
        }
        return AssignmentDelta(assigned, removed, version)
    }

    override fun search(
        tenantId: String,
        subjectType: String,
        expression: TagQueryExpression,
        page: TagKeysetPage,
    ): AssignmentSearchResult {
        val codes = expression.tagCodes()
        val tagIdsByCode = tagDao.listByTenantAndCodes(tenantId, codes)
            .filter { it.subjectType == subjectType }
            .associate { it.code to it.id }
        require(tagIdsByCode.keys.containsAll(codes)) {
            "Every query tag must exist for tenant [$tenantId] and subject type [$subjectType]."
        }
        val rows = assignmentDao.searchSubjectIds(
            queryCompiler.compile(tenantId, subjectType, expression, tagIdsByCode, page)
        )
        val subjectIds = rows.take(page.size)
        return AssignmentSearchResult(
            subjectIds,
            subjectIds.lastOrNull().takeIf { rows.size > page.size },
        )
    }

    private fun TagQueryExpression.tagCodes(): Set<String> = when (this) {
        is TagQueryExpression.AllTags -> tagCodes + nested.flatMapTo(linkedSetOf()) { it.tagCodes() }
        is TagQueryExpression.AnyTags -> tagCodes + nested.flatMapTo(linkedSetOf()) { it.tagCodes() }
        is TagQueryExpression.NotTags -> child.tagCodes()
    }
}
