package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.RegExpEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Matches] validation tests
 *
 * @author K
 * @since 1.0.0
 */
internal class MatchesTest {

    @Test
    fun validVarName_passes() {
        val bean = TestBean("_a1")
        assertTrue(ValidationKit.validateBean(bean).isEmpty())
    }

    @Test
    fun invalidVarName_fails() {
        val bean = TestBean("9bad")
        assertTrue(ValidationKit.validateBean(bean, failFast = false).isNotEmpty())
    }

    @Test
    fun nullValue_passes() {
        val bean = TestBean(null)
        assertTrue(ValidationKit.validateBean(bean).isEmpty())
    }

    @Test
    fun blankMessage_fallsBackToDefaultMessageKey() {
        val bean = TestDefaultMessageBean("9bad")
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertEquals(RegExpEnum.VAR_NAME.defaultMessageKey, violations.first().message)
    }

    internal data class TestBean(
        @get:Matches(value = RegExpEnum.VAR_NAME, message = "invalid var")
        val name: String?,
    )

    internal data class TestDefaultMessageBean(
        @get:Matches(value = RegExpEnum.VAR_NAME)
        val name: String?,
    )
}
