package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * FixedLength 测试用例
 *
 * @author AI: Cursor
 * @since 1.0.0
 */
internal class FixedLengthTest {

    @Test
    fun validateSuccess() {
        val bean = TestBean("abc")
        val violations = ValidationKit.validateBean(bean)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun validateViolationTooLong() {
        val bean = TestBean("abcd")
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertEquals(1, violations.size)
        assertEquals("code须恰好3个字符", violations.first().message)
    }

    @Test
    fun validateViolationTooShort() {
        val bean = TestBean("ab")
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertEquals(1, violations.size)
        assertEquals("code须恰好3个字符", violations.first().message)
    }

    internal data class TestBean(
        @get:FixedLength(3, message = "code须恰好3个字符")
        val code: String?
    )
}
