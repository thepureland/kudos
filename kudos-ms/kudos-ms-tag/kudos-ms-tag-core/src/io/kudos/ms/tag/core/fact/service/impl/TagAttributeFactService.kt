package io.kudos.ms.tag.core.fact.service.impl

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.TagAttributeView
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.fact.model.AttributeFactResult
import io.kudos.ms.tag.core.fact.model.AttributeFactStatus
import io.kudos.ms.tag.core.fact.model.AttributeFactValidationException
import io.kudos.ms.tag.core.fact.service.iservice.ITagAttributeFactService
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeEventDao
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeEvent
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.RecalculationRequest
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.sql.SQLTransientException
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
open class TagAttributeFactService(
    private val subjectTypeDao: TagSubjectTypeDao,
    private val attributeDao: TagAttributeDefinitionDao,
    private val eventDao: TagAttributeEventDao,
    private val subjectDao: TagSubjectDao,
    private val stateStore: AttributeStateStore,
    private val dependencyDao: TagRuleDependencyDao,
    private val ruleDao: TagRuleDao,
    private val recalculationQueue: RecalculationQueue,
    private val transactionExecutor: TagFactTransactionExecutor,
) : ITagAttributeFactService {

    override fun submitFact(fact: TagAttributeFact): AttributeFactResult =
        transactionExecutor.execute { submitFactInTransaction(fact) }

    private fun submitFactInTransaction(fact: TagAttributeFact): AttributeFactResult {
        val checksum = checksum(fact)
        eventDao.get(fact.eventId)?.let { existing ->
            if (existing.payloadChecksum != checksum) {
                fail(TagErrorCode.IDEMPOTENCY_CONFLICT, "Event [${fact.eventId}] was replayed with a different payload.")
            }
            return AttributeFactResult(fact.eventId, AttributeFactStatus.DUPLICATE)
        }

        val definition = validate(fact)
        ensureSubject(fact)
        val event = fact.toEvent(definition.id, checksum)
        eventDao.insert(event)
        val applied = stateStore.apply(fact, definition.toView())
        if (applied.applied) requestAffectedRules(fact, definition)
        event.processStatus = "APPLIED"
        check(eventDao.update(event)) { "Attribute event [${fact.eventId}] could not be marked APPLIED." }
        return AttributeFactResult(fact.eventId, AttributeFactStatus.APPLIED, applied.stateVersion)
    }

    override fun submitFacts(facts: List<TagAttributeFact>, sliceSize: Int): List<AttributeFactResult> {
        require(sliceSize > 0) { "Fact transaction slice size must be positive." }
        val results = arrayOfNulls<AttributeFactResult>(facts.size)
        val validFacts = buildList {
            facts.forEachIndexed { index, fact ->
                try {
                    validate(fact)
                    add(IndexedValue(index, fact))
                } catch (error: AttributeFactValidationException) {
                    results[index] = AttributeFactResult(
                        fact.eventId,
                        AttributeFactStatus.REJECTED,
                        errorCode = error.errorCode,
                        errorMessage = error.message,
                    )
                }
            }
        }
        validFacts.chunked(sliceSize).forEach { slice ->
            try {
                val applied = transactionExecutor.execute {
                    slice.map { indexed -> submitFactInTransaction(indexed.value) }
                }
                slice.zip(applied).forEach { (indexed, result) -> results[indexed.index] = result }
            } catch (error: SQLTransientException) {
                slice.forEach { indexed ->
                    results[indexed.index] = AttributeFactResult(
                        indexed.value.eventId,
                        AttributeFactStatus.RETRYABLE_FAILED,
                        errorMessage = error.message,
                    )
                }
            }
        }
        return results.map { requireNotNull(it) { "Every submitted fact must produce one result." } }
    }

    private fun validate(fact: TagAttributeFact): TagAttributeDefinition {
        if (fact.sourceCode.isBlank()) fail(TagErrorCode.INVALID_ATTRIBUTE_FACT, "Attribute fact source code must not be blank.")
        val subjectType = subjectTypeDao.findByCode(fact.subjectKey.subjectType)
            ?.takeIf { it.active }
            ?: fail(TagErrorCode.INVALID_SUBJECT_TYPE, "Subject type [${fact.subjectKey.subjectType}] does not exist or is inactive.")
        check(subjectType.code == fact.subjectKey.subjectType)
        val definition = attributeDao.findByCode(fact.subjectKey.tenantId, fact.subjectKey.subjectType, fact.attributeCode)
            ?.takeIf { it.active }
            ?: fail(TagErrorCode.INVALID_ATTRIBUTE_CODE, "Attribute [${fact.attributeCode}] does not exist or is inactive.")
        val expectedType = definition.valueType
        val value = fact.value
        if (value != null && value.type() != expectedType) {
            fail(TagErrorCode.INVALID_ATTRIBUTE_VALUE, "Attribute [${definition.code}] requires $expectedType values.")
        }
        val allowed = when {
            definition.cardinality == TagAttributeCardinality.MULTIPLE -> MULTIPLE_OPERATIONS
            definition.valueType == TagAttributeType.INTEGER || definition.valueType == TagAttributeType.DECIMAL -> NUMERIC_SINGLE_OPERATIONS
            else -> BASIC_SINGLE_OPERATIONS
        }
        if (fact.operation !in allowed) {
            fail(
                TagErrorCode.INVALID_ATTRIBUTE_FACT,
                "Operation [${fact.operation}] is invalid for ${definition.valueType}/${definition.cardinality}.",
            )
        }
        val sourceVersion = fact.sourceVersion
        if (fact.operation in ORDERED_OPERATIONS && sourceVersion != null) {
            val latest = eventDao.latestAppliedSourceVersion(fact.subjectKey, definition.id)
            if (latest != null && sourceVersion <= latest) {
                fail(TagErrorCode.INVALID_ATTRIBUTE_FACT, "Attribute fact sourceVersion [$sourceVersion] is not newer than [$latest].")
            }
        }
        return definition
    }

    private fun ensureSubject(fact: TagAttributeFact) {
        if (subjectDao.find(fact.subjectKey) != null) return
        val now = LocalDateTime.now(ZoneOffset.UTC)
        check(subjectDao.insertSubject(TagSubject().apply {
            subjectId = fact.subjectKey.subjectId
            tenantId = fact.subjectKey.tenantId
            subjectType = fact.subjectKey.subjectType
            displayName = null
            profileJson = null
            stateVersion = 0
            firstSeenTime = now
            updateTime = now
        })) { "Subject [${fact.subjectKey}] could not be created." }
    }

    private fun requestAffectedRules(fact: TagAttributeFact, definition: TagAttributeDefinition) {
        dependencyDao.listByTenant(fact.subjectKey.tenantId)
            .asSequence()
            .filter { it.dependencyType == "ATTRIBUTE" && it.attributeId == definition.id }
            .mapNotNull { ruleDao.get(it.ruleId) }
            .filter { it.status == "PUBLISHED" }
            .distinctBy { it.id }
            .forEach { rule ->
                recalculationQueue.request(
                    RecalculationRequest(
                        tenantId = fact.subjectKey.tenantId,
                        jobType = RecalculationJobType.SUBJECT_INCREMENTAL,
                        tagId = rule.tagId,
                        ruleVersion = rule.ruleVersion,
                        subjectType = fact.subjectKey.subjectType,
                        subjectId = fact.subjectKey.subjectId,
                    )
                )
            }
    }

    private fun TagAttributeFact.toEvent(attributeId: String, checksum: String): TagAttributeEvent {
        val received = LocalDateTime.now(ZoneOffset.UTC)
        return TagAttributeEvent().also { event ->
            event.eventId = eventId
            event.requestId = null
            event.payloadChecksum = checksum
            event.tenantId = subjectKey.tenantId
            event.subjectType = subjectKey.subjectType
            event.subjectId = subjectKey.subjectId
            event.attributeId = attributeId
            event.operation = operation.name
            event.sourceCode = sourceCode
            event.sourceVersion = sourceVersion
            event.occurredTime = occurredAt.atOffset(ZoneOffset.UTC).toLocalDateTime()
            event.receivedTime = received
            event.processStatus = "RECEIVED"
            event.errorCode = null
            event.errorMessage = null
            event.valueType = value?.type()?.name
            event.stringValue = (value as? TagAttributeValue.StringValue)?.value
            event.integerValue = (value as? TagAttributeValue.IntegerValue)?.value
            event.decimalValue = (value as? TagAttributeValue.DecimalValue)?.toBigDecimal()
            event.booleanValue = (value as? TagAttributeValue.BooleanValue)?.value
            event.dateValue = (value as? TagAttributeValue.DateValue)?.toLocalDate()
            event.datetimeValue = (value as? TagAttributeValue.DateTimeValue)?.toInstant()?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
        }
    }

    private fun checksum(fact: TagAttributeFact): String = MessageDigest.getInstance("SHA-256")
        .digest(JSON.encodeToString(fact).toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun TagAttributeDefinition.toView() = TagAttributeView(
        id, tenantId, subjectType, code, name, valueType, cardinality,
    )

    private fun TagAttributeValue.type() = when (this) {
        is TagAttributeValue.StringValue -> TagAttributeType.STRING
        is TagAttributeValue.IntegerValue -> TagAttributeType.INTEGER
        is TagAttributeValue.DecimalValue -> TagAttributeType.DECIMAL
        is TagAttributeValue.BooleanValue -> TagAttributeType.BOOLEAN
        is TagAttributeValue.DateValue -> TagAttributeType.DATE
        is TagAttributeValue.DateTimeValue -> TagAttributeType.DATETIME
    }

    private fun fail(errorCode: TagErrorCode, message: String): Nothing =
        throw AttributeFactValidationException(errorCode, message)

    private companion object {
        val BASIC_SINGLE_OPERATIONS = setOf(TagAttributeOperation.SET, TagAttributeOperation.CLEAR)
        val NUMERIC_SINGLE_OPERATIONS = BASIC_SINGLE_OPERATIONS + TagAttributeOperation.ADD
        val MULTIPLE_OPERATIONS = setOf(
            TagAttributeOperation.SET, TagAttributeOperation.APPEND, TagAttributeOperation.REMOVE, TagAttributeOperation.CLEAR,
        )
        val ORDERED_OPERATIONS = setOf(TagAttributeOperation.SET, TagAttributeOperation.CLEAR)
        val JSON = Json { encodeDefaults = true; explicitNulls = true }
    }
}
