package io.kudos.ms.auth.common.role.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleUserErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, the i18nKeyPrefix, the IErrorCodeEnum.displayText
 * contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleUserErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        AuthRoleUserErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, AuthRoleUserErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        AuthRoleUserErrorCodeEnum.entries.forEach { entry ->
            assertEquals("auth.error-msg.roleuser", entry.i18nKeyPrefix)
            assertEquals("auth.error-msg.roleuser.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", AuthRoleUserErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals(
            "Role-user relationship does not exist",
            AuthRoleUserErrorCodeEnum.ROLE_USER_NOT_FOUND.defaultDisplayText,
        )
        assertEquals(
            "The user already holds this role",
            AuthRoleUserErrorCodeEnum.ROLE_USER_ALREADY_EXISTS.defaultDisplayText,
        )
        assertEquals(
            AuthRoleUserErrorCodeEnum.ROLE_USER_ALREADY_EXISTS,
            AuthRoleUserErrorCodeEnum.valueOf("ROLE_USER_ALREADY_EXISTS"),
        )
    }
}
