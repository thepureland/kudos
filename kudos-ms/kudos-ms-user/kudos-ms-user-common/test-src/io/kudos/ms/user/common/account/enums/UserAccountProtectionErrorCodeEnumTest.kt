package io.kudos.ms.user.common.account.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserAccountProtectionErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountProtectionErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserAccountProtectionErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.protection", it.i18nKeyPrefix)
            assertEquals("user.error-msg.protection.${it.code}", it.displayText)
        }
        assertEquals(4, UserAccountProtectionErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals("Account is locked", UserAccountProtectionErrorCodeEnum.ACCOUNT_LOCKED.defaultDisplayText)
        assertEquals(
            "Password error count exceeded threshold",
            UserAccountProtectionErrorCodeEnum.PASSWORD_RETRY_EXCEEDED.defaultDisplayText,
        )
        assertEquals(
            UserAccountProtectionErrorCodeEnum.PROTECTION_NOT_FOUND,
            UserAccountProtectionErrorCodeEnum.valueOf("PROTECTION_NOT_FOUND"),
        )
    }

}
