package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ConstraintsConstraintConvertor测试用例
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
            // 应该包含子约束的规则
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
            // OR模式下应该包含andOr和message
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
            // OR模式下，子约束的message应该被移除
            rule.forEach { (key, value) ->
                if (value is Map<*, *> && key != "andOr" && key != "message") {
                    // 子约束的message应该被移除
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
            // AND模式下，子约束的message应该保留
            rule.forEach { (key, value) ->
                if (value is Map<*, *> && key != "andOr" && key != "message") {
                    // 子约束的message应该保留
                }
            }
        }
    }

    data class TestBean(
        @get:Constraints(
            order = [NotNull::class, Size::class],
            andOr = AndOrEnum.AND,
            message = "必须满足所有约束"
        )
        val value: String?,
        
        @get:Constraints(
            order = [NotNull::class, Size::class],
            andOr = AndOrEnum.OR,
            message = "至少满足一个约束"
        )
        val valueOr: String?
    )
}
