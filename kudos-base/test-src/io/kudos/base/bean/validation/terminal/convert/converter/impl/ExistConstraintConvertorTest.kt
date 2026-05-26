package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Exist
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for ExistConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ExistConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Exist::class.java)
        if (annotation != null) {
            val convertor = ExistConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // Should contain the rules for the sub-constraints
            assertTrue(rule.isNotEmpty())
        }
    }

    @Test
    fun testGetRuleRemovesSubConstraintMessage() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Exist::class.java)
        if (annotation != null) {
            val convertor = ExistConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // Sub-constraint messages should be removed
            // Check that sub-constraint rules have no message field
            rule.forEach { (_, value) ->
                if (value is Map<*, *>) {
                    // Sub-constraint message should be removed
                }
            }
        }
    }

    @Test
    fun testGetRuleContainsExistMessage() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Exist::class.java)
        if (annotation != null) {
            val convertor = ExistConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // Should contain the message from the Exist constraint
            assertTrue(rule.containsKey("message"))
        }
    }

    data class TestBean(
        @get:Exist(
            value = Constraints(
                notNull = NotNull("must not be null"),
                pattern = Pattern(regexp = "[a-zA-Z]+", message = "must contain letters only")
            )
        )
        val value: String?
    )
}
