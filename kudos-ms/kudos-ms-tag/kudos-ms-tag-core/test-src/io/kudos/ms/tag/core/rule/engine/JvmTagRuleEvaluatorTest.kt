package io.kudos.ms.tag.core.rule.engine

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.TagEvaluationContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class JvmTagRuleEvaluatorTest {

    private val evaluator = JvmTagRuleEvaluator()

    @Test
    fun evaluatesBooleanGroupsMissingValuesAndActiveTags() {
        val context = context(
            attributes = mapOf(
                "enabled" to listOf(bool(true)),
                "empty" to emptyList(),
            ),
            activeTags = setOf("trusted"),
        )

        assertTrue(evaluate(all(p("enabled", TagRuleOperator.EQ, bool(true)), TagRuleExpression.HasTag("trusted")), context))
        assertTrue(evaluate(any(p("missing", TagRuleOperator.EXISTS), TagRuleExpression.HasTag("trusted")), context))
        assertTrue(evaluate(TagRuleExpression.Not(TagRuleExpression.HasTag("blocked")), context))
        assertTrue(evaluate(p("enabled", TagRuleOperator.EXISTS), context))
        assertFalse(evaluate(p("empty", TagRuleOperator.EXISTS), context))
        assertTrue(evaluate(p("missing", TagRuleOperator.NOT_EXISTS), context))
        assertFalse(evaluate(p("missing", TagRuleOperator.EQ, string("x")), context))
        assertFalse(evaluate(p("missing", TagRuleOperator.NE, string("x")), context))
        assertFalse(evaluate(p("missing", TagRuleOperator.NOT_IN, string("x")), context))
    }

    @Test
    fun evaluatesSingleAndMultipleEqualityMembershipAndContains() {
        val context = context(
            attributes = mapOf(
                "title" to listOf(string("online gomoku room")),
                "roles" to listOf(string("admin"), string("editor")),
            ),
            cardinalities = mapOf(
                "title" to TagAttributeCardinality.SINGLE,
                "roles" to TagAttributeCardinality.MULTIPLE,
            ),
        )

        assertTrue(evaluate(p("roles", TagRuleOperator.EQ, string("editor")), context))
        assertFalse(evaluate(p("roles", TagRuleOperator.NE, string("editor")), context))
        assertTrue(evaluate(p("roles", TagRuleOperator.NE, string("viewer")), context))
        assertTrue(evaluate(p("roles", TagRuleOperator.IN, string("viewer"), string("admin")), context))
        assertTrue(evaluate(p("roles", TagRuleOperator.NOT_IN, string("viewer"), string("owner")), context))
        assertTrue(evaluate(p("title", TagRuleOperator.CONTAINS, string("gomoku")), context))
        assertTrue(evaluate(p("roles", TagRuleOperator.CONTAINS, string("admin")), context))
        assertFalse(evaluate(p("roles", TagRuleOperator.CONTAINS, string("min")), context))
    }

    @Test
    fun evaluatesOrderedTypesWithDecimalScaleAndUtcDatetimeSemantics() {
        val context = context(
            attributes = mapOf(
                "score" to listOf(integer(40)),
                "area" to listOf(decimal("100.00")),
                "born" to listOf(date("1990-01-02")),
                "seen" to listOf(datetime("2026-09-14T03:04:05+09:00")),
            )
        )

        assertTrue(evaluate(p("score", TagRuleOperator.GT, integer(39)), context))
        assertTrue(evaluate(p("score", TagRuleOperator.GTE, integer(40)), context))
        assertTrue(evaluate(p("score", TagRuleOperator.LT, integer(41)), context))
        assertTrue(evaluate(p("score", TagRuleOperator.LTE, integer(40)), context))
        assertTrue(evaluate(p("score", TagRuleOperator.BETWEEN, integer(30), integer(40)), context))
        assertTrue(evaluate(p("area", TagRuleOperator.EQ, decimal("100.0")), context))
        assertTrue(evaluate(p("born", TagRuleOperator.BETWEEN, date("1990-01-01"), date("1990-01-02")), context))
        assertTrue(evaluate(p("seen", TagRuleOperator.EQ, datetime("2026-09-13T18:04:05Z")), context))
        assertTrue(evaluate(p("seen", TagRuleOperator.GT, datetime("2026-09-13T18:04:04Z")), context))
        assertFalse(evaluate(p("seen", TagRuleOperator.LT, datetime("2026-09-13T18:04:05Z")), context))
    }

    @Test
    fun reproducesHouseGameAndAgeSegments() {
        val houseRule = all(
            p("bedrooms", TagRuleOperator.EQ, integer(3)),
            p("area", TagRuleOperator.GTE, decimal("100")),
            any(
                p("terrace", TagRuleOperator.EQ, bool(true)),
                p("rooftop", TagRuleOperator.EQ, bool(true)),
            ),
        )
        assertTrue(
            evaluate(
                houseRule,
                context(
                    mapOf(
                        "bedrooms" to listOf(integer(3)),
                        "area" to listOf(decimal("118.50")),
                        "terrace" to listOf(bool(false)),
                        "rooftop" to listOf(bool(true)),
                    )
                ),
            )
        )

        val gameRule = all(
            p("gameplay", TagRuleOperator.EQ, string("gomoku")),
            p("player_mode", TagRuleOperator.EQ, string("1v1")),
            p("online", TagRuleOperator.EQ, bool(true)),
        )
        assertTrue(
            evaluate(
                gameRule,
                context(
                    mapOf(
                        "gameplay" to listOf(string("gomoku")),
                        "player_mode" to listOf(string("1v1")),
                        "online" to listOf(bool(true)),
                    )
                ),
            )
        )

        val ageRule = p("age", TagRuleOperator.BETWEEN, integer(30), integer(40))
        assertTrue(evaluate(ageRule, context(mapOf("age" to listOf(integer(30))))))
        assertTrue(evaluate(ageRule, context(mapOf("age" to listOf(integer(40))))))
        assertFalse(evaluate(ageRule, context(mapOf("age" to listOf(integer(41))))))
    }

    private fun evaluate(expression: TagRuleExpression, context: TagEvaluationContext) =
        evaluator.evaluate(expression, context)

    private fun context(
        attributes: Map<String, List<TagAttributeValue>>,
        cardinalities: Map<String, TagAttributeCardinality> = emptyMap(),
        activeTags: Set<String> = emptySet(),
    ) = TagEvaluationContext(SUBJECT, attributes, cardinalities, activeTags)

    private fun p(code: String, operator: TagRuleOperator, vararg values: TagAttributeValue) =
        TagRuleExpression.AttributePredicate(code, operator, values.toList())

    private fun all(vararg children: TagRuleExpression) = TagRuleExpression.AllOf(children.toList())
    private fun any(vararg children: TagRuleExpression) = TagRuleExpression.AnyOf(children.toList())
    private fun string(value: String) = TagAttributeValue.StringValue(value)
    private fun integer(value: Long) = TagAttributeValue.IntegerValue(value)
    private fun decimal(value: String) = TagAttributeValue.DecimalValue(value)
    private fun bool(value: Boolean) = TagAttributeValue.BooleanValue(value)
    private fun date(value: String) = TagAttributeValue.DateValue(value)
    private fun datetime(value: String) = TagAttributeValue.DateTimeValue(value)

    private companion object {
        val SUBJECT = TagSubjectKey("tenant-a", "test.subject", "subject-1")
    }
}
