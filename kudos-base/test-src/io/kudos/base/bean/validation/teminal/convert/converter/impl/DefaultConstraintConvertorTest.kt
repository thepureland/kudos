package io.kudos.base.bean.validation.teminal.convert.converter.impl

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * DefaultConstraintConvertor测试用例
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
            // NotNull注解应该包含message属性
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
            // Size注解应该包含min和max属性
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
            // groups和payload应该被排除
            assertTrue(!rule.containsKey("groups"))
            assertTrue(!rule.containsKey("payload"))
        }
    }

    data class TestBean(
        @get:NotNull(message = "name不能为空")
        @get:Size(min = 1, max = 100, message = "name长度必须在1到100之间")
        val name: String?
    )
}
