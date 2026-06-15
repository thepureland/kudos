package io.kudos.ms.auth.common.group.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for AuthGroupErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, the i18nKeyPrefix, the IErrorCodeEnum.displayText
 * contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        AuthGroupErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, AuthGroupErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        AuthGroupErrorCodeEnum.entries.forEach { entry ->
            assertEquals("auth.error-msg.group", entry.i18nKeyPrefix)
            assertEquals("auth.error-msg.group.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", AuthGroupErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("User group does not exist", AuthGroupErrorCodeEnum.GROUP_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "Group code already exists under this tenant",
            AuthGroupErrorCodeEnum.GROUP_CODE_ALREADY_EXISTS.defaultDisplayText,
        )
        assertEquals(
            AuthGroupErrorCodeEnum.GROUP_NOT_FOUND,
            AuthGroupErrorCodeEnum.valueOf("GROUP_NOT_FOUND"),
        )
    }
}
