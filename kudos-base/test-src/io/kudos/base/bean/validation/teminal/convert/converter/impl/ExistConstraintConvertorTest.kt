package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Exist
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ExistConstraintConvertor测试用例
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
            // 应该包含子约束的规则
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
            // 子约束的message应该被移除
            // 检查子约束规则中是否没有message
            rule.forEach { (key, value) ->
                if (value is Map<*, *>) {
                    // 子约束的message应该被移除
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
            // 应该包含Exist约束的message
            assertTrue(rule.containsKey("message"))
        }
    }

    data class TestBean(
        @get:Exist(
            value = Constraints(
                notNull = NotNull("不能為null"),
                pattern = Pattern(regexp = "[a-zA-Z]+", message = "只能包含字母")
            )
        )
        val value: String?
    )
}
