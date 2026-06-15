package io.kudos.ms.user.common.login.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserLogLoginErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLogLoginErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserLogLoginErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.loglogin", it.i18nKeyPrefix)
            assertEquals("user.error-msg.loglogin.${it.code}", it.displayText)
        }
        assertEquals(2, UserLogLoginErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals("Login log does not exist", UserLogLoginErrorCodeEnum.LOG_LOGIN_NOT_FOUND.defaultDisplayText)
        assertEquals(UserLogLoginErrorCodeEnum.UNSPECIFIED, UserLogLoginErrorCodeEnum.valueOf("UNSPECIFIED"))
    }

}
