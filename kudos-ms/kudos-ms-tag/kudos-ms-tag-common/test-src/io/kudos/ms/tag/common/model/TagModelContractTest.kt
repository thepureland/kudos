package io.kudos.ms.tag.common.model

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.TagValidationException
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class TagModelContractTest {

    private val json = Json {
        classDiscriminator = "type"
    }

    @Test
    fun ruleRoundTripRetainsSubtypeDiscriminatorsAndDecimalPrecision() {
        val key = TagSubjectKey("tenant-a", "estate.house", "house-42")
        val rule: TagRuleExpression = TagRuleExpression.AllOf(
            listOf(
                TagRuleExpression.AttributePredicate(
                    "bedrooms",
                    TagRuleOperator.EQ,
                    listOf(TagAttributeValue.IntegerValue(3)),
                ),
                TagRuleExpression.AttributePredicate(
                    "area",
                    TagRuleOperator.GTE,
                    listOf(TagAttributeValue.DecimalValue("100.0")),
                ),
                TagRuleExpression.AnyOf(
                    listOf(
                        TagRuleExpression.AttributePredicate(
                            "terrace",
                            TagRuleOperator.EQ,
                            listOf(TagAttributeValue.BooleanValue(true)),
                        ),
                        TagRuleExpression.AttributePredicate(
                            "rooftop",
                            TagRuleOperator.EQ,
                            listOf(TagAttributeValue.BooleanValue(true)),
                        ),
                    )
                ),
            )
        )

        val encoded = json.encodeToString(rule)
        val restored = json.decodeFromString<TagRuleExpression>(encoded)

        assertEquals("tenant-a", key.tenantId)
        assertEquals(rule, restored)
        assertTrue(encoded.contains("\"type\":\"all\""), encoded)
        assertTrue(encoded.contains("\"type\":\"any\""), encoded)
        assertTrue(encoded.contains("\"type\":\"attribute\""), encoded)
        assertTrue(encoded.contains("\"value\":\"100.0\""), encoded)
        val decimal = ((restored as TagRuleExpression.AllOf).children[1]
            as TagRuleExpression.AttributePredicate).operands.single()
            as TagAttributeValue.DecimalValue
        assertEquals(BigDecimal("100.0"), decimal.toBigDecimal())
    }

    @Test
    fun subjectKeyRejectsBlankPartsAndInvalidNamespacedType() {
        assertCode(TagErrorCode.INVALID_SUBJECT_KEY) {
            TagSubjectKey("", "estate.house", "house-42")
        }
        assertCode(TagErrorCode.INVALID_SUBJECT_KEY) {
            TagSubjectKey("tenant-a", "estate.house", " ")
        }
        assertCode(TagErrorCode.INVALID_SUBJECT_TYPE) {
            TagSubjectKey("tenant-a", "", "house-42")
        }
        assertCode(TagErrorCode.INVALID_SUBJECT_TYPE) {
            TagSubjectKey("tenant-a", "house", "house-42")
        }
        assertCode(TagErrorCode.INVALID_SUBJECT_TYPE) {
            TagSubjectKey("tenant-a", "Estate.House", "house-42")
        }
    }

    @Test
    fun emptyRuleGroupsAndInvalidBetweenOperandsUseStableCodes() {
        assertCode(TagErrorCode.EMPTY_RULE_GROUP) {
            TagRuleExpression.AllOf(emptyList())
        }
        assertCode(TagErrorCode.EMPTY_RULE_GROUP) {
            TagRuleExpression.AnyOf(emptyList())
        }
        assertCode(TagErrorCode.INVALID_OPERAND_COUNT) {
            TagRuleExpression.AttributePredicate(
                "age",
                TagRuleOperator.BETWEEN,
                listOf(TagAttributeValue.IntegerValue(30)),
            )
        }
        assertCode(TagErrorCode.INVALID_OPERAND_COUNT) {
            TagRuleExpression.AttributePredicate(
                "age",
                TagRuleOperator.BETWEEN,
                listOf(
                    TagAttributeValue.IntegerValue(30),
                    TagAttributeValue.IntegerValue(40),
                    TagAttributeValue.IntegerValue(50),
                ),
            )
        }
    }

    @Test
    fun queryGroupsAreRecursiveAndRejectEmptyOperands() {
        val query: TagQueryExpression = TagQueryExpression.AllTags(
            tagCodes = setOf("estate.three-bedroom", "estate.large"),
            nested = listOf(
                TagQueryExpression.AnyTags(
                    tagCodes = setOf("estate.terrace", "estate.rooftop")
                )
            ),
        )

        assertEquals(query, json.decodeFromString<TagQueryExpression>(json.encodeToString(query)))
        assertCode(TagErrorCode.EMPTY_QUERY_GROUP) {
            TagQueryExpression.AllTags()
        }
        assertCode(TagErrorCode.EMPTY_QUERY_GROUP) {
            TagQueryExpression.AnyTags()
        }
    }

    @Test
    fun attributeFactRoundTripsInstantAsIsoString() {
        val fact = TagAttributeFact(
            eventId = "event-1",
            subjectKey = TagSubjectKey("tenant-a", "hr.person", "person-1"),
            attributeCode = "age",
            operation = TagAttributeOperation.SET,
            value = TagAttributeValue.IntegerValue(35),
            occurredAt = Instant.parse("2026-09-14T01:02:03.456Z"),
            sourceCode = "hr.person-service",
            sourceVersion = 7,
        )

        val encoded = json.encodeToString(fact)
        assertTrue(encoded.contains("2026-09-14T01:02:03.456Z"), encoded)
        assertEquals(fact, json.decodeFromString<TagAttributeFact>(encoded))
    }

    private fun assertCode(expected: TagErrorCode, block: () -> Unit) {
        val error = assertFailsWith<TagValidationException>(block = block)
        assertEquals(expected, error.errorCode)
    }
}
