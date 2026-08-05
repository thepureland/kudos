package io.kudos.ms.sys.common.param.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysParamErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysParamErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysParamErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysParamErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.param", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.param.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Parameter does not exist", SysParamErrorCodeEnum.PARAM_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysParamErrorCodeEnum.PARAM_ALREADY_EXISTS,
            SysParamErrorCodeEnum.valueOf("PARAM_ALREADY_EXISTS")
        )
    }

}
