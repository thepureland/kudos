package io.kudos.ms.tag.core.runtime

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentResolver
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentService
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentTransactionExecutor
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.fact.service.impl.TagAttributeFactService
import io.kudos.ms.tag.core.fact.service.impl.TagFactTransactionExecutor
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.dao.TagRuleNodeDao
import io.kudos.ms.tag.core.rule.dao.TagRuleOperandDao
import io.kudos.ms.tag.core.rule.engine.JvmTagRuleEvaluator
import io.kudos.ms.tag.core.rule.model.TagRuleStatus
import io.kudos.ms.tag.core.rule.service.impl.TagRuleService
import io.kudos.ms.tag.core.rule.validation.TagRuleValidator
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagManualAssignmentEventDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeEventDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeStateDao
import io.kudos.ms.tag.core.runtime.job.TagCandidateBuilder
import io.kudos.ms.tag.core.runtime.job.TagCandidatePromoter
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationCandidateDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import io.kudos.ms.tag.core.runtime.rdb.RdbAttributeStateStore
import io.kudos.ms.tag.core.runtime.rdb.RdbRecalculationQueue
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.core.runtime.rdb.RdbTagMembershipStore
import io.kudos.ms.tag.core.runtime.service.impl.TagRecalculationService
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FullRecalculationServiceTest : TagDaoTestSupport() {

    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val tagDao = TagDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val ruleDao = TagRuleDao()
    private val dependencyDao = TagRuleDependencyDao()
    private val subjectDao = TagSubjectDao()
    private val jobDao = TagRecalculationJobDao()
    private val candidateDao = TagRecalculationCandidateDao()
    private val queue = RdbRecalculationQueue(jobDao)
    private val stateStore = RdbAttributeStateStore(TagAttributeStateDao(), attributeDao, subjectDao)
    private val ruleService = TagRuleService(
        tagDao,
        attributeDao,
        ruleDao,
        TagRuleNodeDao(),
        TagRuleOperandDao(),
        dependencyDao,
        TagRuleValidator(attributeDao, tagDao, ruleDao, dependencyDao),
        queue,
    )
    private val assignmentDao = TagAssignmentDao()
    private val assignmentEventDao = TagAssignmentEventDao()
    private val assignmentService = TagAssignmentService(
        tagDao,
        tagSetDao,
        subjectDao,
        TagMembershipDao(),
        RdbTagMembershipStore(TagMembershipDao()),
        RdbTagAssignmentIndex(assignmentDao, assignmentEventDao, tagDao, tagSetDao),
        TagAssignmentResolver(),
        assignmentEventDao,
        TagManualAssignmentEventDao(),
        TagAssignmentTransactionExecutor(),
    )
    private val builder = TagCandidateBuilder(
        tagDao,
        ruleDao,
        ruleService,
        subjectDao,
        attributeDao,
        stateStore,
        assignmentDao,
        JvmTagRuleEvaluator(),
        candidateDao,
        jobDao,
        TagAssignmentTransactionExecutor(),
    )
    private val promoter = TagCandidatePromoter(
        tagDao,
        ruleDao,
        ruleService,
        subjectDao,
        candidateDao,
        assignmentService,
        jobDao,
        TagAssignmentTransactionExecutor(),
    )

    @Test
    fun candidateScanResumesWithoutChangingLiveV1AndPromotesOnlyAfterExhaustion() {
        prepareCatalog()
        val v1 = publishInitialRule(gte(30))
        submitAge("person-a", 35)
        submitAge("person-b", 45)
        val recalculation = TagRecalculationService(
            tagDao,
            attributeDao,
            ruleDao,
            dependencyDao,
            ruleService,
            stateStore,
            assignmentDao,
            assignmentService,
            JvmTagRuleEvaluator(),
            queue,
        )
        recalculation.recalculateSubjectNow(listOf(key("person-a"), key("person-b")))
        assertEquals(setOf("person-a", "person-b"), assignedSubjectIds())

        val v2 = ruleService.createDraft(TENANT, TAG_CODE, gte(40))
        val publication = ruleService.requestPublication(TENANT, v2.id)
        var lease = leaseRebuild(publication.rebuildJobId)

        val first = builder.build(lease, batchSize = 1)
        assertEquals("person-a", first.cursorSubjectId)
        assertFalse(first.exhausted)
        assertEquals(emptyList(), candidateDao.listByRun(lease.id))
        assertEquals(setOf("person-a", "person-b"), assignedSubjectIds())
        assertEquals(v1.id, tagDao.findByCode(TENANT, SUBJECT_TYPE, TAG_CODE)?.publishedRuleId)

        lease = leaseRebuild(publication.rebuildJobId)
        val second = builder.build(lease, batchSize = 1)
        assertEquals("person-b", second.cursorSubjectId)
        assertFalse(second.exhausted)
        assertEquals(listOf("person-b"), candidateDao.listByRun(lease.id).map { it.subjectId })
        assertEquals(setOf("person-a", "person-b"), assignedSubjectIds())

        lease = leaseRebuild(publication.rebuildJobId)
        val last = builder.build(lease, batchSize = 1)
        assertTrue(last.exhausted)
        promoter.promote(lease.id)

        assertEquals(setOf("person-b"), assignedSubjectIds())
        assertEquals(v2.id, tagDao.findByCode(TENANT, SUBJECT_TYPE, TAG_CODE)?.publishedRuleId)
        assertEquals(TagRuleStatus.PUBLISHED.name, ruleDao.findByTenantAndId(TENANT, v2.id)?.status)
        assertEquals(TagRuleStatus.RETIRED.name, ruleDao.findByTenantAndId(TENANT, v1.id)?.status)
        assertEquals("SUCCEEDED", jobDao.get(lease.id)?.status)
    }

    @Test
    fun cancelledRebuildCannotPromoteOrReplaceThePublishedVersion() {
        prepareCatalog()
        val v1 = publishInitialRule(gte(30))
        submitAge("person-a", 35)
        val v2 = ruleService.createDraft(TENANT, TAG_CODE, gte(40))
        val publication = ruleService.requestPublication(TENANT, v2.id)
        val lease = leaseRebuild(publication.rebuildJobId)
        builder.build(lease, batchSize = 10)
        assertTrue(queue.cancel(lease.id))

        assertFailsWith<IllegalArgumentException> { promoter.promote(lease.id) }
        assertEquals(v1.id, tagDao.findByCode(TENANT, SUBJECT_TYPE, TAG_CODE)?.publishedRuleId)
        assertEquals(TagRuleStatus.REBUILDING.name, ruleDao.findByTenantAndId(TENANT, v2.id)?.status)
    }

    private fun prepareCatalog() {
        val catalog = TagCatalogService(subjectTypeDao, attributeDao, tagSetDao, tagDao)
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "Person", "hr"))
        catalog.createAttribute(
            CreateTagAttributeCommand(
                TENANT,
                SUBJECT_TYPE,
                "age",
                "Age",
                TagAttributeType.INTEGER,
                TagAttributeCardinality.SINGLE,
            )
        )
        catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, TAG_CODE, "Working age", manualAssignable = false))
    }

    private fun publishInitialRule(expression: TagRuleExpression) =
        ruleService.createDraft(TENANT, TAG_CODE, expression).also { draft ->
            val publication = ruleService.requestPublication(TENANT, draft.id)
            ruleService.completePublication(TENANT, draft.id)
            check(queue.cancel(publication.rebuildJobId))
        }

    private fun submitAge(subjectId: String, age: Long) {
        val factService = TagAttributeFactService(
            subjectTypeDao,
            attributeDao,
            TagAttributeEventDao(),
            subjectDao,
            stateStore,
            dependencyDao,
            ruleDao,
            queue,
            TagFactTransactionExecutor(),
        )
        factService.submitFact(
            TagAttributeFact(
                "age-$subjectId",
                key(subjectId),
                "age",
                TagAttributeOperation.SET,
                TagAttributeValue.IntegerValue(age),
                Instant.parse("2026-09-14T00:00:00Z"),
                "hr",
            )
        )
        jobDao.listByTenant(TENANT)
            .filter { it.subjectId == subjectId && it.status == "PENDING" }
            .forEach { check(queue.cancel(it.id)) }
    }

    private fun assignedSubjectIds() = assignmentDao.searchSubjectIds(
        io.kudos.ms.tag.core.runtime.rdb.CompiledTagAssignmentQuery(
            "select subject_id from tag_assignment where tenant_id = ? and subject_type = ? order by subject_id",
            listOf(TENANT, SUBJECT_TYPE),
        )
    ).toSet()

    private fun leaseRebuild(jobId: String) = (1..3).firstNotNullOfOrNull {
        queue.lease("rebuild-worker", 10, Instant.now().plusSeconds(60)).firstOrNull { job -> job.id == jobId }
    } ?: error("Full-rebuild job [$jobId] was not leaseable.")

    private fun gte(age: Long) = TagRuleExpression.AttributePredicate(
        "age",
        TagRuleOperator.GTE,
        listOf(TagAttributeValue.IntegerValue(age)),
    )

    private fun key(subjectId: String) = TagSubjectKey(TENANT, SUBJECT_TYPE, subjectId)

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "hr.person"
        const val TAG_CODE = "working_age"
    }
}
