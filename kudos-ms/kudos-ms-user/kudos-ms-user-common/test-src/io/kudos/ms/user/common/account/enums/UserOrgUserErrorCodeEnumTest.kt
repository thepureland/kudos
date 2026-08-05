package io.kudos.ms.user.common.account.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserOrgUserErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgUserErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserOrgUserErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.orguser", it.i18nKeyPrefix)
            assertEquals("user.error-msg.orguser.${it.code}", it.displayText)
        }
        assertEquals(3, UserOrgUserErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals(
            "Organization-user relationship not found",
            UserOrgUserErrorCodeEnum.ORG_USER_NOT_FOUND.defaultDisplayText,
        )
        assertEquals(
            UserOrgUserErrorCodeEnum.ORG_USER_ALREADY_EXISTS,
            UserOrgUserErrorCodeEnum.valueOf("ORG_USER_ALREADY_EXISTS"),
        )
    }

}
