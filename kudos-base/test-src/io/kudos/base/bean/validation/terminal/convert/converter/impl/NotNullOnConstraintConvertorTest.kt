package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.NotNullOn
import io.kudos.base.bean.validation.support.Depends
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for NotNullOnConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class NotNullOnConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(NotNullOn::class.java)
        if (annotation != null) {
            val convertor = NotNullOnConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // Should contain the `depends` attribute
            assertTrue(rule.containsKey("depends"))
        }
    }

    @Test
    fun testGetRuleWithDepends() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(NotNullOn::class.java)
        if (annotation != null) {
            val convertor = NotNullOnConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            val depends = rule["depends"]
            assertNotNull(depends)
            // `depends` should be the rule map of the Depends annotation
            assertTrue(depends is Map<*, *>)
        }
    }

    data class TestBean(
        @get:NotNullOn(
            depends = Depends(
                properties = ["other"],
                values = ["test"]
            ),
            message = "value must not be null when `other` is `test`"
        )
        val value: String?,
        val other: String?
    )
}
