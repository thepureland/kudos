package io.kudos.ms.user.common.login.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserLoginRememberMeErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLoginRememberMeErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserLoginRememberMeErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.loginremember", it.i18nKeyPrefix)
            assertEquals("user.error-msg.loginremember.${it.code}", it.displayText)
        }
        assertEquals(3, UserLoginRememberMeErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals("Undefined error", UserLoginRememberMeErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals(
            "Remember-me token is invalid",
            UserLoginRememberMeErrorCodeEnum.REMEMBER_ME_TOKEN_INVALID.defaultDisplayText,
        )
        assertEquals(
            UserLoginRememberMeErrorCodeEnum.REMEMBER_ME_NOT_FOUND,
            UserLoginRememberMeErrorCodeEnum.valueOf("REMEMBER_ME_NOT_FOUND"),
        )
    }

}
