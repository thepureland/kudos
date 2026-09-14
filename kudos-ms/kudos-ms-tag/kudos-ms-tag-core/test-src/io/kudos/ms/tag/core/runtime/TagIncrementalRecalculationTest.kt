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
import io.kudos.ms.tag.core.rule.service.impl.TagRuleService
import io.kudos.ms.tag.core.rule.validation.TagRuleValidator
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagManualAssignmentEventDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeEventDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeStateDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.port.TagEvaluationContext
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator
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

internal class TagIncrementalRecalculationTest : TagDaoTestSupport() {

    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val tagDao = TagDefinitionDao()
    private val ruleDao = TagRuleDao()
    private val nodeDao = TagRuleNodeDao()
    private val operandDao = TagRuleOperandDao()
    private val dependencyDao = TagRuleDependencyDao()
    private val subjectDao = TagSubjectDao()
    private val stateStore = RdbAttributeStateStore(TagAttributeStateDao(), attributeDao, subjectDao)
    private val jobDao = TagRecalculationJobDao()
    private val queue = RdbRecalculationQueue(jobDao)
    private val catalog = TagCatalogService(subjectTypeDao, attributeDao, tagSetDao, tagDao)
    private val rules = TagRuleService(
        tagDao,
        attributeDao,
        ruleDao,
        nodeDao,
        operandDao,
        dependencyDao,
        TagRuleValidator(attributeDao, tagDao, ruleDao, dependencyDao),
        queue,
    )
    private val assignmentDao = TagAssignmentDao()
    private val assignmentEventDao = TagAssignmentEventDao()
    private val membershipStore = RdbTagMembershipStore(TagMembershipDao())
    private val assignments = TagAssignmentService(
        tagDao,
        tagSetDao,
        subjectDao,
        TagMembershipDao(),
        membershipStore,
        RdbTagAssignmentIndex(assignmentDao, assignmentEventDao, tagDao, tagSetDao),
        TagAssignmentResolver(),
        assignmentEventDao,
        TagManualAssignmentEventDao(),
        TagAssignmentTransactionExecutor(),
    )
    private val evaluationOrder = mutableListOf<String>()
    private val evaluator = object : TagRuleEvaluator {
        private val delegate = JvmTagRuleEvaluator()

        override fun evaluate(expression: TagRuleExpression, context: TagEvaluationContext): Boolean {
            evaluationOrder += when (expression) {
                is TagRuleExpression.AttributePredicate -> "attribute:${expression.attributeCode}"
                is TagRuleExpression.HasTag -> "tag:${expression.tagCode}"
                else -> expression::class.simpleName.orEmpty()
            }
            return delegate.evaluate(expression, context)
        }
    }
    private val service = TagRecalculationService(
        tagDao,
        attributeDao,
        ruleDao,
        dependencyDao,
        rules,
        stateStore,
        assignmentDao,
        assignments,
        evaluator,
        queue,
    )

    @Test
    fun changedAttributeEvaluatesOnlyDirectPublishedRulesThenCascadesInDependencyOrder() {
        prepareCatalogAndRules()
        val key = key("cascade")
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
                "age-cascade",
                key,
                "age",
                TagAttributeOperation.SET,
                TagAttributeValue.IntegerValue(35),
                Instant.parse("2026-09-14T00:00:00Z"),
                "hr",
            )
        )

        val incrementalJobs = jobDao.listByTenant(TENANT).filter { it.jobType == RecalculationJobType.SUBJECT_INCREMENTAL.name }
        assertEquals(listOf("working_age"), incrementalJobs.map { requireNotNull(tagDao.get(it.tagId)).code })
        val leased = queue.lease("worker-1", 10, Instant.now().plusSeconds(60)).single()
        val first = service.process(leased)

        assertEquals(listOf("attribute:age", "tag:working_age"), evaluationOrder)
        assertEquals(setOf("working_age", "experienced"), assignmentCodes(key))
        assertEquals(1, first.directRulesEvaluated)
        assertEquals(1, first.cascadedRulesEvaluated)
        assertEquals(2, first.assignmentChanges)

        val eventCount = assignmentEventDao.list(key).size
        evaluationOrder.clear()
        val unchanged = service.process(leased)

        assertEquals(listOf("attribute:age"), evaluationOrder)
        assertEquals(0, unchanged.assignmentChanges)
        assertEquals(eventCount, assignmentEventDao.list(key).size)
    }

    @Test
    fun synchronousAndWorkerEntryPointsEnforceTheirSafetyBounds() {
        prepareCatalogAndRules()
        val keys = (1..101).map { key("person-$it") }

        assertFailsWith<IllegalArgumentException> { service.recalculateSubjectNow(keys) }
        assertFailsWith<IllegalArgumentException> { service.recalculateSubjectNow(listOf(key("one")), directRuleLimit = 0) }
        assertFailsWith<IllegalArgumentException> { service.recalculateSubjectNow(listOf(key("one")), directRuleLimit = 201) }
        assertFailsWith<IllegalArgumentException> {
            service.process(
                LeasedRecalculationJob(
                    id = "full-rebuild",
                    tenantId = TENANT,
                    jobType = RecalculationJobType.RULE_FULL_REBUILD,
                    tagId = requireNotNull(tagDao.findByCode(TENANT, SUBJECT_TYPE, "working_age")).id,
                    ruleVersion = 1,
                    subjectType = SUBJECT_TYPE,
                    subjectId = null,
                    cursorSubjectId = null,
                    requestedVersion = 1,
                    processedVersion = 0,
                    leaseOwner = "worker-1",
                    leaseUntil = Instant.now().plusSeconds(60),
                    version = 1,
                )
            )
        }
    }

    private fun prepareCatalogAndRules() {
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
        catalog.createAttribute(
            CreateTagAttributeCommand(
                TENANT,
                SUBJECT_TYPE,
                "department",
                "Department",
                TagAttributeType.STRING,
                TagAttributeCardinality.SINGLE,
            )
        )
        listOf("working_age", "experienced", "engineering").forEach { code ->
            catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, code, code, manualAssignable = false))
        }
        publish(
            "working_age",
            TagRuleExpression.AttributePredicate(
                "age",
                TagRuleOperator.BETWEEN,
                listOf(TagAttributeValue.IntegerValue(30), TagAttributeValue.IntegerValue(40)),
            ),
        )
        publish("experienced", TagRuleExpression.HasTag("working_age"))
        publish(
            "engineering",
            TagRuleExpression.AttributePredicate(
                "department",
                TagRuleOperator.EQ,
                listOf(TagAttributeValue.StringValue("engineering")),
            ),
        )
    }

    private fun publish(tagCode: String, expression: TagRuleExpression) {
        val draft = rules.createDraft(TENANT, tagCode, expression)
        val publication = rules.requestPublication(TENANT, draft.id)
        rules.completePublication(TENANT, draft.id)
        check(queue.cancel(publication.rebuildJobId))
    }

    private fun assignmentCodes(key: TagSubjectKey): Set<String> =
        assignmentDao.list(key).map { requireNotNull(tagDao.get(it.tagId)).code }.toSet()

    private fun key(subjectId: String) = TagSubjectKey(TENANT, SUBJECT_TYPE, subjectId)

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "hr.person"
    }
}
