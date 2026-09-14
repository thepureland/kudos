package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.TagAttributeView
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeStateDao
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeState
import io.kudos.ms.tag.core.runtime.port.AttributeApplyResult
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

open class RdbAttributeStateStore(
    private val stateDao: TagAttributeStateDao,
    private val attributeDao: TagAttributeDefinitionDao,
    private val subjectDao: TagSubjectDao,
) : AttributeStateStore {

    override fun apply(fact: TagAttributeFact, definition: TagAttributeView): AttributeApplyResult {
        val subject = requireNotNull(subjectDao.find(fact.subjectKey)) { "Subject must exist before applying attribute state." }
        val current = stateDao.list(fact.subjectKey, definition.id)
        val nextVersion = subject.stateVersion + 1
        val applied = when (fact.operation) {
            TagAttributeOperation.CLEAR -> {
                current.forEach { stateDao.deleteById(it.id) }
                current.isNotEmpty()
            }
            TagAttributeOperation.SET -> {
                current.forEach { stateDao.deleteById(it.id) }
                insertState(fact, definition, requireNotNull(fact.value), nextVersion)
                true
            }
            TagAttributeOperation.ADD -> {
                val delta = requireNotNull(fact.value)
                val value = when (delta) {
                    is TagAttributeValue.IntegerValue -> TagAttributeValue.IntegerValue(
                        ((current.singleOrNull()?.toValue() as? TagAttributeValue.IntegerValue)?.value ?: 0) + delta.value
                    )
                    is TagAttributeValue.DecimalValue -> TagAttributeValue.DecimalValue(
                        ((current.singleOrNull()?.toValue() as? TagAttributeValue.DecimalValue)?.toBigDecimal()
                            ?: java.math.BigDecimal.ZERO).add(delta.toBigDecimal()).toPlainString()
                    )
                    else -> error("ADD requires an integer or decimal value.")
                }
                current.forEach { stateDao.deleteById(it.id) }
                insertState(fact, definition, value, nextVersion)
                true
            }
            TagAttributeOperation.APPEND -> {
                val value = requireNotNull(fact.value)
                if (current.none { it.valueKey == valueKey(value) }) {
                    insertState(fact, definition, value, nextVersion)
                    true
                } else {
                    false
                }
            }
            TagAttributeOperation.REMOVE -> {
                val key = valueKey(requireNotNull(fact.value))
                val removed = current.filter { it.valueKey == key }
                removed.forEach { stateDao.deleteById(it.id) }
                removed.isNotEmpty()
            }
        }
        if (applied) {
            check(
                subjectDao.advanceStateVersion(
                    fact.subjectKey,
                    expectedVersion = subject.stateVersion,
                    nextVersion = nextVersion,
                    updateTime = LocalDateTime.now(ZoneOffset.UTC),
                )
            ) { "Subject state version could not be advanced because it changed concurrently." }
        }
        return AttributeApplyResult(
            applied,
            if (applied) nextVersion else subject.stateVersion,
            load(fact.subjectKey, setOf(definition.code))[definition.code].orEmpty(),
        )
    }

    override fun load(key: TagSubjectKey, attributeCodes: Set<String>): Map<String, List<TagAttributeValue>> =
        attributeCodes.associateWith { code ->
            attributeDao.findByCode(key.tenantId, key.subjectType, code)
                ?.let { definition -> stateDao.list(key, definition.id).map { it.toValue() } }
                .orEmpty()
        }

    private fun insertState(
        fact: TagAttributeFact,
        definition: TagAttributeView,
        value: TagAttributeValue,
        stateVersion: Long,
    ) {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        stateDao.insert(TagAttributeState().also { state ->
            state.id = UUID.randomUUID().toString()
            state.tenantId = fact.subjectKey.tenantId
            state.subjectType = fact.subjectKey.subjectType
            state.subjectId = fact.subjectKey.subjectId
            state.attributeId = definition.id
            state.valueKey = if (definition.cardinality == TagAttributeCardinality.SINGLE) SINGLE_VALUE_KEY else valueKey(value)
            state.valueType = value.typeName()
            state.stringValue = (value as? TagAttributeValue.StringValue)?.value
            state.integerValue = (value as? TagAttributeValue.IntegerValue)?.value
            state.decimalValue = (value as? TagAttributeValue.DecimalValue)?.toBigDecimal()
            state.booleanValue = (value as? TagAttributeValue.BooleanValue)?.value
            state.dateValue = (value as? TagAttributeValue.DateValue)?.toLocalDate()
            state.datetimeValue = (value as? TagAttributeValue.DateTimeValue)?.toInstant()?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
            state.sourceEventId = fact.eventId
            state.sourceVersion = fact.sourceVersion
            state.stateVersion = stateVersion
            state.effectiveTime = fact.occurredAt.atOffset(ZoneOffset.UTC).toLocalDateTime()
            state.expireTime = null
            state.updateTime = now
        })
    }

    private fun valueKey(value: TagAttributeValue): String {
        val canonical = when (value) {
            is TagAttributeValue.StringValue -> "STRING:${value.value}"
            is TagAttributeValue.IntegerValue -> "INTEGER:${value.value}"
            is TagAttributeValue.DecimalValue -> "DECIMAL:${value.toBigDecimal().stripTrailingZeros().toPlainString()}"
            is TagAttributeValue.BooleanValue -> "BOOLEAN:${value.value}"
            is TagAttributeValue.DateValue -> "DATE:${value.toLocalDate()}"
            is TagAttributeValue.DateTimeValue -> "DATETIME:${value.toInstant()}"
        }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun TagAttributeValue.typeName() = when (this) {
        is TagAttributeValue.StringValue -> "STRING"
        is TagAttributeValue.IntegerValue -> "INTEGER"
        is TagAttributeValue.DecimalValue -> "DECIMAL"
        is TagAttributeValue.BooleanValue -> "BOOLEAN"
        is TagAttributeValue.DateValue -> "DATE"
        is TagAttributeValue.DateTimeValue -> "DATETIME"
    }

    private fun TagAttributeState.toValue(): TagAttributeValue = when (valueType) {
        "STRING" -> TagAttributeValue.StringValue(requireNotNull(stringValue))
        "INTEGER" -> TagAttributeValue.IntegerValue(requireNotNull(integerValue))
        "DECIMAL" -> TagAttributeValue.DecimalValue(requireNotNull(decimalValue).stripTrailingZeros().toPlainString())
        "BOOLEAN" -> TagAttributeValue.BooleanValue(requireNotNull(booleanValue))
        "DATE" -> TagAttributeValue.DateValue(requireNotNull(dateValue).toString())
        "DATETIME" -> TagAttributeValue.DateTimeValue(requireNotNull(datetimeValue).atOffset(ZoneOffset.UTC).toString())
        else -> error("Unsupported persisted attribute type [$valueType].")
    }

    private companion object {
        const val SINGLE_VALUE_KEY = "_single"
    }
}
