package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.RegExpEnum
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [Matches] 校验测试
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

    internal data class TestBean(
        @get:Matches(value = RegExpEnum.VAR_NAME, message = "invalid var")
        val name: String?,
    )
}
