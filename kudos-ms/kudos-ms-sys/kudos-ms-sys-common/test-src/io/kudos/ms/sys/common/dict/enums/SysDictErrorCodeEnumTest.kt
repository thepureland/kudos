package io.kudos.ms.sys.common.dict.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysDictErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, and the
 * IErrorCodeEnum.displayText contract (prefix + "." + code when the prefix is not blank).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysDictErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(5, SysDictErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysDictErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.dict", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.dict.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("DICT_NOT_FOUND", SysDictErrorCodeEnum.DICT_NOT_FOUND.code)
        assertEquals("Dictionary not found", SysDictErrorCodeEnum.DICT_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysDictErrorCodeEnum.DICT_ITEM_CODE_ALREADY_EXISTS,
            SysDictErrorCodeEnum.valueOf("DICT_ITEM_CODE_ALREADY_EXISTS")
        )
    }

}
