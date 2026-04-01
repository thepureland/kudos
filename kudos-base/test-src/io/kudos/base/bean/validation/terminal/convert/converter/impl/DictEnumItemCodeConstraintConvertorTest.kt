package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.enums.impl.SexEnum
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * DictEnumCodeConstraintConvertor测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class DictEnumItemCodeConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("sex")
            .getAnnotation(DictEnumItemCode::class.java)
        if (annotation != null) {
            val convertor = DictEnumCodeConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // 应该包含values属性
            assertTrue(rule.containsKey("values"))
            // enumClass应该被移除
            assertFalse(rule.containsKey("enumClass"))
        }
    }

    @Test
    fun testGetRuleWithEnumValues() {
        val annotation = TestBean::class.java.getDeclaredField("sex")
            .getAnnotation(DictEnumItemCode::class.java)
        if (annotation != null) {
            val convertor = DictEnumCodeConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            val values = rule["values"]
            assertNotNull(values)
            // values应该是枚举的code集合
        }
    }

    data class TestBean(
        @get:DictEnumItemCode(enumClass = SexEnum::class, message = "无效的性别")
        val sex: String?
    )
}
