package io.kudos.ms.sys.common.datasource.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysDataSourceErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysDataSourceErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysDataSourceErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysDataSourceErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.datasource", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.datasource.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Data source does not exist", SysDataSourceErrorCodeEnum.DATA_SOURCE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysDataSourceErrorCodeEnum.DATA_SOURCE_ALREADY_EXISTS,
            SysDataSourceErrorCodeEnum.valueOf("DATA_SOURCE_ALREADY_EXISTS")
        )
    }

}
