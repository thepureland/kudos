package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.enums.LogParamTypeEnum
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [LogParamVo].
 *
 * Coverage:
 *  - All four constructors (no-arg / name+value / +type / +locale) populate exactly the fields they declare.
 *  - The default [LogParamVo.type] is [LogParamTypeEnum.STRING]'s code, matching the "plain string param" common case.
 *  - [LogParamVo.getLocale] / [LogParamVo.setLocale] round-trip, including null reset.
 *  - Null and Unicode values are carried verbatim — the VO is a dumb holder, no normalization allowed.
 *
 * @author K
 * @since 1.0.0
 */
internal class LogParamVoTest {

    @Test
    fun noArgConstructor_defaultsToStringType() {
        val vo = LogParamVo()
        assertNull(vo.name)
        assertNull(vo.value)
        assertEquals(LogParamTypeEnum.STRING.code, vo.type, "default type must be the STRING dict code")
        assertNull(vo.getLocale())
    }

    @Test
    fun nameValueConstructor_keepsDefaultType() {
        val vo = LogParamVo("user", "alice")
        assertEquals("user", vo.name)
        assertEquals("alice", vo.value)
        assertEquals(LogParamTypeEnum.STRING.code, vo.type)
        assertNull(vo.getLocale())
    }

    @Test
    fun typeConstructor_overridesDefaultType() {
        val vo = LogParamVo("amount", 100.5, LogParamTypeEnum.CURRENCY.code)
        assertEquals("amount", vo.name)
        assertEquals(100.5, vo.value)
        assertEquals(LogParamTypeEnum.CURRENCY.code, vo.type)
    }

    @Test
    fun localeConstructor_populatesAllFields() {
        val vo = LogParamVo("when", "2024-01-01", LogParamTypeEnum.DATE.code, Locale.TRADITIONAL_CHINESE)
        assertEquals("when", vo.name)
        assertEquals("2024-01-01", vo.value)
        assertEquals(LogParamTypeEnum.DATE.code, vo.type)
        assertEquals(Locale.TRADITIONAL_CHINESE, vo.getLocale())
    }

    @Test
    fun setLocale_roundTrips_andAcceptsNullReset() {
        val vo = LogParamVo()
        vo.setLocale(Locale.JAPAN)
        assertEquals(Locale.JAPAN, vo.getLocale())
        vo.setLocale(null)
        assertNull(vo.getLocale())
    }

    @Test
    fun nullAndUnicodeValues_carriedVerbatim() {
        val vo = LogParamVo(null, null, null)
        assertNull(vo.name)
        assertNull(vo.value)
        assertNull(vo.type)

        val unicode = LogParamVo("名稱", "值┼emoji😀")
        assertEquals("名稱", unicode.name)
        assertEquals("值┼emoji😀", unicode.value)
    }
}
