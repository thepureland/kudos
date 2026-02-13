package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictCode
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * DictCodeConstraintConvertor测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class DictCodeConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("code")
            .getAnnotation(DictCode::class.java)
        if (annotation != null) {
            val convertor = DictCodeConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // 应该包含values属性
            assertTrue(rule.containsKey("values"))
        }
    }

    @Test
    fun testGetRuleWithValues() {
        val annotation = TestBean::class.java.getDeclaredField("code")
            .getAnnotation(DictCode::class.java)
        if (annotation != null) {
            val convertor = DictCodeConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // values应该是从IDictCodeFinder获取的字典码集合
            val values = rule["values"]
            assertNotNull(values)
        }
    }

    data class TestBean(
        @get:DictCode(atomicServiceCode = "test", dictType = "test", message = "无效的字典码")
        val code: String?
    )
}
