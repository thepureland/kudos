package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.base.bean.validation.support.RegExps
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import jakarta.validation.constraints.Pattern
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * MatchesConstraintConvertor 测试
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
internal class MatchesConstraintConvertorTest {

    @Test
    fun getRule_emulatesPatternKeys() {
        val prop = TestBean::class.memberProperties.first { it.name == "code" }
        val annotation = prop.getter.annotations.filterIsInstance<Matches>().first()
        val convertor = MatchesConstraintConvertor(annotation)
        val rule = convertor.getRule(annotation)
        println(rule)
        assertEquals(RegExps.CharacterSet.VAR_NAME, rule["regexp"])
        assertEquals("sys.valid-msg.default.Pattern::var-name", rule["message"])
        assertEquals(0, rule["flags"])
        assertFalse(rule.containsKey("value"))
    }

    @Test
    fun convert_usesPatternAsConstraintName() {
        val prop = TerminalBean::class.memberProperties.first { it.name == "code" }
        val annotation = prop.getter.annotations.filterIsInstance<Matches>().first()
        val ctx = ConstraintConvertContext("code", null, TerminalBean::class)
        val terminal = MatchesConstraintConvertor(annotation).convert(ctx)
        assertEquals(Pattern::class.java.simpleName, terminal.constraint)
        assertEquals(RegExps.CharacterSet.VAR_NAME, terminal.rule.single()["regexp"])
    }

    internal data class TestBean(
        @get:Matches(RegExpEnum.VAR_NAME)
        val code: String?,
    )

    internal data class TerminalBean(
        @get:Matches(RegExpEnum.VAR_NAME)
        val code: String?,
    )
}
