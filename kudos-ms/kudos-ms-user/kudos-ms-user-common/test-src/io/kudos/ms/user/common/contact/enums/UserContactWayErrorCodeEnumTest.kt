package io.kudos.ms.user.common.contact.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for UserContactWayErrorCodeEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayErrorCodeEnumTest {

    @Test
    fun codeEqualsNameAndTextNotBlank() {
        UserContactWayErrorCodeEnum.entries.forEach {
            assertEquals(it.name, it.code)
            assertTrue(it.defaultDisplayText.isNotBlank())
            assertEquals("user.error-msg.contact", it.i18nKeyPrefix)
            assertEquals("user.error-msg.contact.${it.code}", it.displayText)
        }
        assertEquals(3, UserContactWayErrorCodeEnum.entries.size)
    }

    @Test
    fun specificEntries() {
        assertEquals("Contact way not found", UserContactWayErrorCodeEnum.CONTACT_WAY_NOT_FOUND.defaultDisplayText)
        assertEquals(
            UserContactWayErrorCodeEnum.CONTACT_WAY_ALREADY_EXISTS,
            UserContactWayErrorCodeEnum.valueOf("CONTACT_WAY_ALREADY_EXISTS"),
        )
    }

}
