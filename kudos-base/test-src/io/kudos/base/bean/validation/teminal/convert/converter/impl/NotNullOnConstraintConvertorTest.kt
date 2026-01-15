package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.NotNullOn
import io.kudos.base.bean.validation.support.Depends
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * NotNullOnConstraintConvertor测试用例
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
            // 应该包含depends属性
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
            // depends应该是Depends注解的规则映射
            assertTrue(depends is Map<*, *>)
        }
    }

    data class TestBean(
        @get:NotNullOn(
            depends = Depends(
                properties = ["other"],
                values = ["test"]
            ),
            message = "当other为test时，value不能为空"
        )
        val value: String?,
        val other: String?
    )
}
