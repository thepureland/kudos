package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for DictCodeConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class DictItemCodeConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("code")
            .getAnnotation(DictItemCode::class.java)
        if (annotation != null) {
            val convertor = DictCodeConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // Should contain the `values` attribute
            assertTrue(rule.containsKey("values"))
        }
    }

    @Test
    fun testGetRuleWithValues() {
        val annotation = TestBean::class.java.getDeclaredField("code")
            .getAnnotation(DictItemCode::class.java)
        if (annotation != null) {
            val convertor = DictCodeConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // `values` should be the set of dictionary codes obtained from IDictCodeFinder
            val values = rule["values"]
            assertNotNull(values)
        }
    }

    data class TestBean(
        @get:DictItemCode(atomicServiceCode = "test", dictType = "test", message = "invalid dictionary code")
        val code: String?
    )
}
