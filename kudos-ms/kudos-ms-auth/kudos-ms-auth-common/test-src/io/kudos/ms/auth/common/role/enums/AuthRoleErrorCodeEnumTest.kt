package io.kudos.ms.auth.common.role.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, the i18nKeyPrefix, the IErrorCodeEnum.displayText
 * contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        AuthRoleErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, AuthRoleErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        AuthRoleErrorCodeEnum.entries.forEach { entry ->
            assertEquals("auth.error-msg.role", entry.i18nKeyPrefix)
            assertEquals("auth.error-msg.role.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", AuthRoleErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Role does not exist", AuthRoleErrorCodeEnum.ROLE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "Role code already exists under this tenant",
            AuthRoleErrorCodeEnum.ROLE_CODE_ALREADY_EXISTS.defaultDisplayText,
        )
        assertEquals(AuthRoleErrorCodeEnum.ROLE_NOT_FOUND, AuthRoleErrorCodeEnum.valueOf("ROLE_NOT_FOUND"))
    }
}
