package io.kudos.ms.user.common.account.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserAccountThirdErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountThirdErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserAccountThirdErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.user-third", it.i18nKeyPrefix)
            assertEquals("user.error-msg.user-third.${it.code}", it.displayText)
        }
        assertEquals(3, UserAccountThirdErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals(
            "Third-party account binding not found",
            UserAccountThirdErrorCodeEnum.THIRD_ACCOUNT_NOT_FOUND.defaultDisplayText,
        )
        assertEquals(
            UserAccountThirdErrorCodeEnum.THIRD_ACCOUNT_ALREADY_BOUND,
            UserAccountThirdErrorCodeEnum.valueOf("THIRD_ACCOUNT_ALREADY_BOUND"),
        )
    }

}
