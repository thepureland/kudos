package io.kudos.ms.user.common.org.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserOrgErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserOrgErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.org", it.i18nKeyPrefix)
            assertEquals("user.error-msg.org.${it.code}", it.displayText)
        }
        assertEquals(4, UserOrgErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals("Organization not found", UserOrgErrorCodeEnum.ORG_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "Parent organization does not exist or is disabled",
            UserOrgErrorCodeEnum.PARENT_ORG_NOT_FOUND.defaultDisplayText,
        )
        assertEquals(
            UserOrgErrorCodeEnum.ORG_CODE_ALREADY_EXISTS,
            UserOrgErrorCodeEnum.valueOf("ORG_CODE_ALREADY_EXISTS"),
        )
    }

}
