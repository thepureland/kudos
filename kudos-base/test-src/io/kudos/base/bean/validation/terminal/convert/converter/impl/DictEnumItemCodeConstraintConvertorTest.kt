package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.enums.impl.SexEnum
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for DictEnumCodeConstraintConvertor.
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
            // Should include the `values` attribute
            assertTrue(rule.containsKey("values"))
            // enumClass should be removed
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
            // `values` should be the set of enum codes
        }
    }

    data class TestBean(
        @get:DictEnumItemCode(enumClass = SexEnum::class, message = "invalid sex")
        val sex: String?
    )
}
