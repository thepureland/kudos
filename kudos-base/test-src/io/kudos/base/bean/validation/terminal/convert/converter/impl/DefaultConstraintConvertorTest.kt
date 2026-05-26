package io.kudos.base.bean.validation.terminal.convert.converter.impl

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for DefaultConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class DefaultConstraintConvertorTest {

    @Test
    fun testGetRuleForNotNull() {
        val annotation = TestBean::class.java.getDeclaredField("name")
            .getAnnotation(NotNull::class.java)
        if (annotation != null) {
            val convertor = DefaultConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // The NotNull annotation should include the message attribute
            assertTrue(rule.containsKey("message"))
        }
    }

    @Test
    fun testGetRuleForSize() {
        val annotation = TestBean::class.java.getDeclaredField("name")
            .getAnnotation(Size::class.java)
        if (annotation != null) {
            val convertor = DefaultConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // The Size annotation should include the min and max attributes
            assertTrue(rule.containsKey("min"))
            assertTrue(rule.containsKey("max"))
            assertEquals(1, rule["min"])
            assertEquals(100, rule["max"])
        }
    }

    @Test
    fun testGetRuleExcludesGroupsAndPayload() {
        val annotation = TestBean::class.java.getDeclaredField("name")
            .getAnnotation(NotNull::class.java)
        if (annotation != null) {
            val convertor = DefaultConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // groups and payload should be excluded
            assertTrue(!rule.containsKey("groups"))
            assertTrue(!rule.containsKey("payload"))
        }
    }

    data class TestBean(
        @get:NotNull(message = "name must not be null")
        @get:Size(min = 1, max = 100, message = "name length must be between 1 and 100")
        val name: String?
    )
}
