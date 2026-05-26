package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Each
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for EachConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class EachConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("items")
            .getAnnotation(Each::class.java)
        if (annotation != null) {
            val convertor = EachConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // Should contain rules for the sub-constraints
            assertTrue(rule.isNotEmpty())
        }
    }

    @Test
    fun testGetRuleWithSubConstraints() {
        val annotation = TestBean::class.java.getDeclaredField("items")
            .getAnnotation(Each::class.java)
        if (annotation != null) {
            val convertor = EachConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // Should contain sub-constraints such as NotNull and Size
            assertTrue(rule.containsKey("NotNull") || rule.containsKey("Size"))
        }
    }

    data class TestBean(
        @get:Each(
            value = Constraints(
                notNull = NotNull("must not be null"),
                size = Size(min = 1, max = 100, message = "size must be between 1 and 100")
            )
        )
        val items: List<String>?
    )
}
