package io.kudos.ms.user.common.account.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserAccountErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserAccountErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.user", it.i18nKeyPrefix)
            assertEquals("user.error-msg.user.${it.code}", it.displayText)
        }
        assertEquals(3, UserAccountErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", UserAccountErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("User not found", UserAccountErrorCodeEnum.USER_NOT_FOUND.defaultDisplayText)
        assertEquals(
            UserAccountErrorCodeEnum.USERNAME_ALREADY_EXISTS,
            UserAccountErrorCodeEnum.valueOf("USERNAME_ALREADY_EXISTS"),
        )
    }

}
