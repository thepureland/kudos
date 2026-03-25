package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.MaxSize
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MaxSize测试用例
 *
 * @author AI
 * @since 1.0.0
 */
internal class MaxSizeTest {

    @Test
    fun validateSuccess() {
        val bean = TestBean("abc")
        val violations = ValidationKit.validateBean(bean)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun validateViolation() {
        val bean = TestBean("abcd")
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertEquals(1, violations.size)
        assertEquals("name长度不能超过3", violations.first().message)
    }

    internal data class TestBean(
        @get:MaxSize(max = 3, message = "name长度不能超过3")
        val name: String?
    )
}
