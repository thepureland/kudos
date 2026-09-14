package io.kudos.ms.tag.core.fact

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.TagAttributeView
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.fact.model.AttributeFactStatus
import io.kudos.ms.tag.core.fact.model.AttributeFactValidationException
import io.kudos.ms.tag.core.fact.service.impl.TagAttributeFactService
import io.kudos.ms.tag.core.fact.service.impl.TagFactTransactionExecutor
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.model.po.TagRule
import io.kudos.ms.tag.core.rule.model.po.TagRuleDependency
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeEventDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeStateDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.port.AttributeApplyResult
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.rdb.RdbAttributeStateStore
import io.kudos.ms.tag.core.runtime.rdb.RdbRecalculationQueue
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import java.time.Instant
import java.sql.SQLTransientException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

internal class TagAttributeFactServiceTest : TagDaoTestSupport() {

    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val eventDao = TagAttributeEventDao()
    private val stateDao = TagAttributeStateDao()
    private val subjectDao = TagSubjectDao()
    private val jobDao = TagRecalculationJobDao()
    private val dependencyDao = TagRuleDependencyDao()
    private val ruleDao = TagRuleDao()
    private val stateStore = RdbAttributeStateStore(stateDao, attributeDao, subjectDao)
    private val service = createService(stateStore)
    private val catalog = TagCatalogService(subjectTypeDao, attributeDao, TagSetDao(), TagDefinitionDao())

    @Test
    fun appliesEveryLegalSingleAndMultipleOperationWithTypedValues() {
        prepareCatalog()
        val singleCases = listOf(
            Triple("name", TagAttributeValue.StringValue("Ada"), TagAttributeValue.StringValue("Ada")),
            Triple("enabled", TagAttributeValue.BooleanValue(false), TagAttributeValue.BooleanValue(false)),
            Triple("born", TagAttributeValue.DateValue("1990-01-02"), TagAttributeValue.DateValue("1990-01-02")),
            Triple(
                "seen",
                TagAttributeValue.DateTimeValue("2026-09-14T03:04:05+09:00"),
                TagAttributeValue.DateTimeValue("2026-09-13T18:04:05Z"),
            ),
        )
        singleCases.forEachIndexed { index, (code, value, expected) ->
            val key = key("single-$index")
            assertEquals(AttributeFactStatus.APPLIED, service.submitFact(fact("$code-set", key, code, TagAttributeOperation.SET, value)).status)
            assertEquals(listOf(expected), stateStore.load(key, setOf(code))[code])
            service.submitFact(fact("$code-clear", key, code, TagAttributeOperation.CLEAR, null))
            assertEquals(emptyList(), stateStore.load(key, setOf(code))[code])
        }

        val scoreKey = key("score")
        service.submitFact(fact("score-set", scoreKey, "score", TagAttributeOperation.SET, TagAttributeValue.IntegerValue(10)))
        service.submitFact(fact("score-add", scoreKey, "score", TagAttributeOperation.ADD, TagAttributeValue.IntegerValue(5)))
        assertEquals(listOf(TagAttributeValue.IntegerValue(15)), stateStore.load(scoreKey, setOf("score"))["score"])
        service.submitFact(fact("score-clear", scoreKey, "score", TagAttributeOperation.CLEAR, null))

        val balanceKey = key("balance")
        service.submitFact(fact("balance-set", balanceKey, "balance", TagAttributeOperation.SET, TagAttributeValue.DecimalValue("10.25")))
        service.submitFact(fact("balance-add", balanceKey, "balance", TagAttributeOperation.ADD, TagAttributeValue.DecimalValue("0.75")))
        assertEquals(listOf(TagAttributeValue.DecimalValue("11")), stateStore.load(balanceKey, setOf("balance"))["balance"])

        val rolesKey = key("roles")
        service.submitFact(fact("roles-set", rolesKey, "roles", TagAttributeOperation.SET, TagAttributeValue.StringValue("admin")))
        service.submitFact(fact("roles-append", rolesKey, "roles", TagAttributeOperation.APPEND, TagAttributeValue.StringValue("editor")))
        service.submitFact(fact("roles-append-duplicate", rolesKey, "roles", TagAttributeOperation.APPEND, TagAttributeValue.StringValue("editor")))
        assertEquals(
            setOf(TagAttributeValue.StringValue("admin"), TagAttributeValue.StringValue("editor")),
            stateStore.load(rolesKey, setOf("roles"))["roles"]?.toSet(),
        )
        service.submitFact(fact("roles-remove", rolesKey, "roles", TagAttributeOperation.REMOVE, TagAttributeValue.StringValue("admin")))
        assertEquals(listOf(TagAttributeValue.StringValue("editor")), stateStore.load(rolesKey, setOf("roles"))["roles"])
        service.submitFact(fact("roles-clear", rolesKey, "roles", TagAttributeOperation.CLEAR, null))
        assertEquals(emptyList(), stateStore.load(rolesKey, setOf("roles"))["roles"])
    }

    @Test
    fun eventIdIsIdempotentAndConflictingReplayDoesNotMutateState() {
        prepareCatalog()
        publishRuleDependingOn("score")
        val key = key("idempotent")
        val original = fact("same-event", key, "score", TagAttributeOperation.ADD, TagAttributeValue.IntegerValue(5))

        assertEquals(AttributeFactStatus.APPLIED, service.submitFact(original).status)
        assertEquals(AttributeFactStatus.DUPLICATE, service.submitFact(original).status)
        assertEquals(listOf(TagAttributeValue.IntegerValue(5)), stateStore.load(key, setOf("score"))["score"])
        assertEquals(1, jobDao.listByTenant(TENANT).single().requestedVersion)

        val error = assertFailsWith<AttributeFactValidationException> {
            service.submitFact(original.copy(value = TagAttributeValue.IntegerValue(6)))
        }
        assertEquals(TagErrorCode.IDEMPOTENCY_CONFLICT, error.errorCode)
        assertEquals(listOf(TagAttributeValue.IntegerValue(5)), stateStore.load(key, setOf("score"))["score"])
        assertEquals(1, jobDao.listByTenant(TENANT).single().requestedVersion)
    }

    @Test
    fun rejectsUnknownMismatchedIllegalAndStaleFactsWithoutPartialEvents() {
        prepareCatalog()
        val key = key("invalid")
        val invalidFacts = listOf(
            fact("unknown", key, "missing", TagAttributeOperation.SET, TagAttributeValue.StringValue("x")),
            fact("wrong-type", key, "score", TagAttributeOperation.SET, TagAttributeValue.StringValue("10")),
            fact("illegal-op", key, "name", TagAttributeOperation.ADD, TagAttributeValue.StringValue("x")),
        )
        invalidFacts.forEach { invalid ->
            assertFailsWith<AttributeFactValidationException> { service.submitFact(invalid) }
            assertEquals(null, eventDao.get(invalid.eventId))
        }

        service.submitFact(fact("newer", key, "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("new"), 2))
        assertFailsWith<AttributeFactValidationException> {
            service.submitFact(fact("older", key, "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("old"), 1))
        }
        assertEquals(null, eventDao.get("older"))
        assertEquals(listOf(TagAttributeValue.StringValue("new")), stateStore.load(key, setOf("name"))["name"])

        service.submitFact(fact("clear-newest", key, "name", TagAttributeOperation.CLEAR, null, 3))
        assertFailsWith<AttributeFactValidationException> {
            service.submitFact(fact("older-after-clear", key, "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("old"), 2))
        }
        assertEquals(null, eventDao.get("older-after-clear"))
        assertEquals(emptyList(), stateStore.load(key, setOf("name"))["name"])
    }

    @Test
    fun successfulFactPersistsSubjectEventAndStateAndBatchIsItemIsolated() {
        prepareCatalog()
        val first = fact("batch-good-1", key("batch-1"), "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("one"))
        val bad = fact("batch-bad", key("batch-2"), "missing", TagAttributeOperation.SET, TagAttributeValue.StringValue("bad"))
        val second = fact("batch-good-2", key("batch-3"), "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("three"))

        val results = service.submitFacts(listOf(first, bad, second), sliceSize = 2)

        assertEquals(listOf(AttributeFactStatus.APPLIED, AttributeFactStatus.REJECTED, AttributeFactStatus.APPLIED), results.map { it.status })
        assertNotNull(subjectDao.find(first.subjectKey))
        assertEquals("APPLIED", eventDao.get(first.eventId)?.processStatus)
        assertEquals(listOf(TagAttributeValue.StringValue("one")), stateStore.load(first.subjectKey, setOf("name"))["name"])
        assertEquals(null, eventDao.get(bad.eventId))
    }

    @Test
    fun transientDatabaseFailureMarksTheWholeBoundedSliceRetryable() {
        prepareCatalog()
        var applications = 0
        val failingStore = object : AttributeStateStore {
            override fun apply(fact: TagAttributeFact, definition: TagAttributeView): AttributeApplyResult {
                applications += 1
                if (applications == 2) throw SQLTransientException("temporary database outage")
                return stateStore.apply(fact, definition)
            }

            override fun load(key: TagSubjectKey, attributeCodes: Set<String>) = stateStore.load(key, attributeCodes)
        }
        val facts = listOf(
            fact("retry-1", key("retry-1"), "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("one")),
            fact("retry-2", key("retry-2"), "name", TagAttributeOperation.SET, TagAttributeValue.StringValue("two")),
        )

        val results = createService(failingStore).submitFacts(facts, sliceSize = 2)

        assertEquals(listOf(AttributeFactStatus.RETRYABLE_FAILED, AttributeFactStatus.RETRYABLE_FAILED), results.map { it.status })
    }

    private fun prepareCatalog() {
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "Person", "hr"))
        createAttribute("name", TagAttributeType.STRING, TagAttributeCardinality.SINGLE)
        createAttribute("enabled", TagAttributeType.BOOLEAN, TagAttributeCardinality.SINGLE)
        createAttribute("born", TagAttributeType.DATE, TagAttributeCardinality.SINGLE)
        createAttribute("seen", TagAttributeType.DATETIME, TagAttributeCardinality.SINGLE)
        createAttribute("score", TagAttributeType.INTEGER, TagAttributeCardinality.SINGLE)
        createAttribute("balance", TagAttributeType.DECIMAL, TagAttributeCardinality.SINGLE)
        createAttribute("roles", TagAttributeType.STRING, TagAttributeCardinality.MULTIPLE)
    }

    private fun createAttribute(code: String, type: TagAttributeType, cardinality: TagAttributeCardinality) {
        catalog.createAttribute(CreateTagAttributeCommand(TENANT, SUBJECT_TYPE, code, code, type, cardinality))
    }

    private fun publishRuleDependingOn(attributeCode: String) {
        val tag = catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "test-$attributeCode", "test-$attributeCode"))
        val rule = TagRule().apply {
            id = UUID.randomUUID().toString()
            tenantId = TENANT
            tagId = tag.id
            ruleVersion = 1
            status = "PUBLISHED"
            rootNodeId = null
            expressionVersion = 1
            checksum = "test-checksum"
            publishedTime = null
            retiredTime = null
            version = 0
        }
        ruleDao.insert(rule)
        dependencyDao.insert(TagRuleDependency().apply {
            id = UUID.randomUUID().toString()
            tenantId = TENANT
            ruleId = rule.id
            tagId = tag.id
            dependencyType = "ATTRIBUTE"
            attributeId = requireNotNull(attributeDao.findByCode(TENANT, SUBJECT_TYPE, attributeCode)).id
            referencedTagId = null
        })
    }

    private fun createService(attributeStateStore: AttributeStateStore) = TagAttributeFactService(
        subjectTypeDao,
        attributeDao,
        eventDao,
        subjectDao,
        attributeStateStore,
        dependencyDao,
        ruleDao,
        RdbRecalculationQueue(jobDao),
        TagFactTransactionExecutor(),
    )

    private fun key(id: String) = TagSubjectKey(TENANT, SUBJECT_TYPE, id)

    private fun fact(
        eventId: String,
        key: TagSubjectKey,
        code: String,
        operation: TagAttributeOperation,
        value: TagAttributeValue?,
        sourceVersion: Long? = null,
    ) = TagAttributeFact(eventId, key, code, operation, value, Instant.parse("2026-09-14T00:00:00Z"), "test", sourceVersion)

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "hr.person"
    }
}
