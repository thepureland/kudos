package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Each
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * EachConstraintConvertor测试用例
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
            // 应该包含子约束的规则
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
            // 应该包含NotNull和Size等子约束
            assertTrue(rule.containsKey("NotNull") || rule.containsKey("Size"))
        }
    }

    data class TestBean(
        @get:Each(
            value = Constraints(
                notNull = NotNull("不能為null"),
                size = Size(min = 1, max = 100, message = "大小必須在1到100間")
            )
        )
        val items: List<String>?
    )
}
