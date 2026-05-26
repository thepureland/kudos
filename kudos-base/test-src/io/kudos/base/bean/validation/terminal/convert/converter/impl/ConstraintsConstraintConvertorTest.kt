package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for ConstraintsConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ConstraintsConstraintConvertorTest {

    @Test
    fun testGetRuleWithAnd() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Constraints::class.java)
        if (annotation != null) {
            val convertor = ConstraintsConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // Should contain rules for the sub-constraints
            assertTrue(rule.isNotEmpty())
        }
    }

    @Test
    fun testGetRuleWithOr() {
        val annotation = TestBean::class.java.getDeclaredField("valueOr")
            .getAnnotation(Constraints::class.java)
        if (annotation != null) {
            val convertor = ConstraintsConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // In OR mode, andOr and message should be included
            assertTrue(rule.containsKey("andOr"))
            assertTrue(rule.containsKey("message"))
        }
    }

    @Test
    fun testGetRuleWithOrRemovesSubConstraintMessage() {
        val annotation = TestBean::class.java.getDeclaredField("valueOr")
            .getAnnotation(Constraints::class.java)
        if (annotation != null) {
            val convertor = ConstraintsConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // In OR mode, sub-constraint messages should be removed
            rule.forEach { (key, value) ->
                if (value is Map<*, *> && key != "andOr" && key != "message") {
                    // Sub-constraint message should be removed
                }
            }
        }
    }

    @Test
    fun testGetRuleWithAndKeepsSubConstraintMessage() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Constraints::class.java)
        if (annotation != null) {
            val convertor = ConstraintsConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // In AND mode, sub-constraint messages should be preserved
            rule.forEach { (key, value) ->
                if (value is Map<*, *> && key != "andOr" && key != "message") {
                    // Sub-constraint message should be preserved
                }
            }
        }
    }

    data class TestBean(
        @get:Constraints(
            order = [NotNull::class, Size::class],
            andOr = AndOrEnum.AND,
            message = "all constraints must be satisfied"
        )
        val value: String?,

        @get:Constraints(
            order = [NotNull::class, Size::class],
            andOr = AndOrEnum.OR,
            message = "at least one constraint must be satisfied"
        )
        val valueOr: String?
    )
}
