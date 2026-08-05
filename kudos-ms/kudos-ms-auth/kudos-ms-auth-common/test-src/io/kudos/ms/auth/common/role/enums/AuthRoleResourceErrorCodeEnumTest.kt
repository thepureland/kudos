package io.kudos.ms.auth.common.role.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleResourceErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, the i18nKeyPrefix, the IErrorCodeEnum.displayText
 * contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleResourceErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        AuthRoleResourceErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, AuthRoleResourceErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        AuthRoleResourceErrorCodeEnum.entries.forEach { entry ->
            assertEquals("auth.error-msg.roleresource", entry.i18nKeyPrefix)
            assertEquals("auth.error-msg.roleresource.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", AuthRoleResourceErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals(
            "Role-resource relationship does not exist",
            AuthRoleResourceErrorCodeEnum.ROLE_RESOURCE_NOT_FOUND.defaultDisplayText,
        )
        assertEquals(
            "The role already holds this resource",
            AuthRoleResourceErrorCodeEnum.ROLE_RESOURCE_ALREADY_EXISTS.defaultDisplayText,
        )
        assertEquals(
            AuthRoleResourceErrorCodeEnum.ROLE_RESOURCE_NOT_FOUND,
            AuthRoleResourceErrorCodeEnum.valueOf("ROLE_RESOURCE_NOT_FOUND"),
        )
    }
}
