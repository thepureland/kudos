package io.kudos.ms.auth.common.group.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for AuthGroupUserErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, the i18nKeyPrefix, the IErrorCodeEnum.displayText
 * contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupUserErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        AuthGroupUserErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, AuthGroupUserErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        AuthGroupUserErrorCodeEnum.entries.forEach { entry ->
            assertEquals("auth.error-msg.groupuser", entry.i18nKeyPrefix)
            assertEquals("auth.error-msg.groupuser.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", AuthGroupUserErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals(
            "Group-user relationship does not exist",
            AuthGroupUserErrorCodeEnum.GROUP_USER_NOT_FOUND.defaultDisplayText,
        )
        assertEquals(
            "The user has already joined this group",
            AuthGroupUserErrorCodeEnum.GROUP_USER_ALREADY_EXISTS.defaultDisplayText,
        )
        assertEquals(
            AuthGroupUserErrorCodeEnum.GROUP_USER_ALREADY_EXISTS,
            AuthGroupUserErrorCodeEnum.valueOf("GROUP_USER_ALREADY_EXISTS"),
        )
    }
}
