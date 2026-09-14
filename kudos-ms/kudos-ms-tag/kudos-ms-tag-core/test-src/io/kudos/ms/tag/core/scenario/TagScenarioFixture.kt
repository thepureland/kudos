package io.kudos.ms.tag.core.scenario

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.api.TagAttributeFactApi
import io.kudos.ms.tag.core.api.TagQueryApi
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
import io.kudos.ms.tag.core.query.service.impl.TagQueryService
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
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.rdb.RdbAttributeStateStore
import io.kudos.ms.tag.core.runtime.rdb.RdbRecalculationQueue
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.core.runtime.rdb.RdbTagMembershipStore
import io.kudos.ms.tag.core.runtime.service.impl.TagRecalculationService
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.ms.tag.core.security.TagSubjectWriteGuard
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import java.time.Instant
import java.util.UUID

/** Real RDB-backed fixture shared by the acceptance scenarios; only security guards are bypassed in local API calls. */
internal class TagScenarioFixture {
    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val tagDao = TagDefinitionDao()
    private val ruleDao = TagRuleDao()
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
        TagRuleNodeDao(),
        TagRuleOperandDao(),
        dependencyDao,
        TagRuleValidator(attributeDao, tagDao, ruleDao, dependencyDao),
        queue,
    )
    private val membershipDao = TagMembershipDao()
    private val assignmentDao = TagAssignmentDao()
    private val assignmentEventDao = TagAssignmentEventDao()
    private val assignmentIndex = RdbTagAssignmentIndex(assignmentDao, assignmentEventDao, tagDao, tagSetDao)
    private val assignments = TagAssignmentService(
        tagDao,
        tagSetDao,
        subjectDao,
        membershipDao,
        RdbTagMembershipStore(membershipDao),
        assignmentIndex,
        TagAssignmentResolver(),
        assignmentEventDao,
        TagManualAssignmentEventDao(),
        TagAssignmentTransactionExecutor(),
    )
    private val factService = TagAttributeFactService(
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
    private val recalculation = TagRecalculationService(
        tagDao,
        attributeDao,
        ruleDao,
        dependencyDao,
        rules,
        stateStore,
        assignmentDao,
        assignments,
        JvmTagRuleEvaluator(),
        queue,
    )
    private val queryService = TagQueryService(tagDao, assignmentIndex)
    private val factApi = TagAttributeFactApi(factService, NoopSubjectWriteGuard)
    private val queryApi = TagQueryApi(queryService, NoopTenantGuard)

    fun configureEstate(tenantId: String) {
        subjectType("estate.house", "House", "estate")
        attribute(tenantId, "estate.house", "bedrooms", TagAttributeType.INTEGER)
        attribute(tenantId, "estate.house", "area", TagAttributeType.DECIMAL)
        attribute(tenantId, "estate.house", "terrace", TagAttributeType.BOOLEAN)
        attribute(tenantId, "estate.house", "rooftop", TagAttributeType.BOOLEAN)
        tag(tenantId, "estate.house", "three_bedrooms", predicate("bedrooms", TagRuleOperator.EQ, TagAttributeValue.IntegerValue(3)))
        tag(tenantId, "estate.house", "area_100_plus", predicate("area", TagRuleOperator.GTE, TagAttributeValue.DecimalValue("100")))
        tag(tenantId, "estate.house", "has_terrace", predicate("terrace", TagRuleOperator.EQ, TagAttributeValue.BooleanValue(true)))
        tag(tenantId, "estate.house", "has_rooftop", predicate("rooftop", TagRuleOperator.EQ, TagAttributeValue.BooleanValue(true)))
    }

    fun configureGame(tenantId: String) {
        subjectType("game.catalog", "Game", "game")
        attribute(tenantId, "game.catalog", "gameplay", TagAttributeType.STRING)
        attribute(tenantId, "game.catalog", "mode", TagAttributeType.STRING)
        attribute(tenantId, "game.catalog", "online", TagAttributeType.BOOLEAN)
        tag(tenantId, "game.catalog", "gomoku", predicate("gameplay", TagRuleOperator.EQ, TagAttributeValue.StringValue("gomoku")))
        tag(tenantId, "game.catalog", "one_v_one", predicate("mode", TagRuleOperator.EQ, TagAttributeValue.StringValue("1v1")))
        tag(tenantId, "game.catalog", "online_play", predicate("online", TagRuleOperator.EQ, TagAttributeValue.BooleanValue(true)))
    }

    fun configureHr(tenantId: String) {
        subjectType("hr.person", "Person", "hr")
        attribute(tenantId, "hr.person", "age", TagAttributeType.INTEGER)
        tag(
            tenantId,
            "hr.person",
            "age_30_40",
            TagRuleExpression.AttributePredicate(
                "age",
                TagRuleOperator.BETWEEN,
                listOf(TagAttributeValue.IntegerValue(30), TagAttributeValue.IntegerValue(40)),
            ),
        )
    }

    fun submit(tenantId: String, subjectType: String, subjectId: String, vararg values: Pair<String, TagAttributeValue>) {
        factService.submitFacts(facts(tenantId, subjectType, subjectId, values))
    }

    fun submitThroughLocalApi(
        tenantId: String,
        subjectType: String,
        subjectId: String,
        vararg values: Pair<String, TagAttributeValue>,
    ) {
        factApi.submitFacts(facts(tenantId, subjectType, subjectId, values))
    }

    fun processPending() {
        val workerId = "scenario-${UUID.randomUUID()}"
        while (true) {
            val leased = queue.lease(workerId, 100, Instant.now().plusSeconds(60))
            if (leased.isEmpty()) return
            leased.forEach { job ->
                check(job.jobType == RecalculationJobType.SUBJECT_INCREMENTAL)
                val result = recalculation.process(job)
                check(queue.complete(job.id, workerId, job.version, job.requestedVersion, result.subjectsProcessed.toLong()))
            }
        }
    }

    fun query(tenantId: String, subjectType: String, expression: TagQueryExpression): List<String> =
        queryService.findSubjects(TagQueryRequest(tenantId, subjectType, expression)).subjectIds

    fun queryThroughLocalApi(tenantId: String, subjectType: String, expression: TagQueryExpression): List<String> =
        queryApi.findSubjects(TagQueryRequest(tenantId, subjectType, expression)).subjectIds

    private fun subjectType(code: String, name: String, owner: String) {
        if (subjectTypeDao.findByCode(code) == null) {
            catalog.registerSubjectType(RegisterTagSubjectTypeCommand(code, name, owner))
        }
    }

    private fun attribute(tenantId: String, subjectType: String, code: String, type: TagAttributeType) {
        if (attributeDao.findByCode(tenantId, subjectType, code) == null) {
            catalog.createAttribute(
                CreateTagAttributeCommand(tenantId, subjectType, code, code, type, TagAttributeCardinality.SINGLE)
            )
        }
    }

    private fun tag(tenantId: String, subjectType: String, code: String, expression: TagRuleExpression) {
        if (tagDao.findByCode(tenantId, subjectType, code) != null) return
        catalog.createTag(CreateTagCommand(tenantId, subjectType, code, code, manualAssignable = false))
        val draft = rules.createDraft(tenantId, code, expression)
        val publication = rules.requestPublication(tenantId, draft.id)
        rules.completePublication(tenantId, draft.id)
        check(queue.cancel(publication.rebuildJobId))
    }

    private fun predicate(code: String, operator: TagRuleOperator, value: TagAttributeValue) =
        TagRuleExpression.AttributePredicate(code, operator, listOf(value))

    private fun facts(
        tenantId: String,
        subjectType: String,
        subjectId: String,
        values: Array<out Pair<String, TagAttributeValue>>,
    ): List<TagAttributeFact> = values.map { (code, value) ->
        TagAttributeFact(
            UUID.randomUUID().toString(),
            TagSubjectKey(tenantId, subjectType, subjectId),
            code,
            TagAttributeOperation.SET,
            value,
            Instant.now(),
            subjectType.substringBefore('.'),
        )
    }

    private object NoopTenantGuard : TagTenantAccessGuard() {
        override fun requireTenant(tenantId: String) = Unit
    }

    private object NoopSubjectWriteGuard : TagSubjectWriteGuard() {
        override fun requireWrite(key: TagSubjectKey) = Unit
    }
}
