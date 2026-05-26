package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test cases for MaxLength.
 *
 * @author AI: Codex
 * @since 1.0.0
 */
internal class MaxLengthTest {

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
        assertEquals("name length must not exceed 3", violations.first().message)
    }

    internal data class TestBean(
        @get:MaxLength(max = 3, message = "name length must not exceed 3")
        val name: String?
    )
}
