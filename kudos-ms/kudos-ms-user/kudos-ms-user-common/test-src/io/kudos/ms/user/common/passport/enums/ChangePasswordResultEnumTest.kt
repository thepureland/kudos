package io.kudos.ms.user.common.passport.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for ChangePasswordResultEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class ChangePasswordResultEnumTest {

    @Test
    fun entriesAndOrder() {
        assertEquals(5, ChangePasswordResultEnum.entries.size)
        assertEquals(
            listOf(
                "SUCCESS",
                "USER_NOT_FOUND",
                "OLD_PASSWORD_WRONG",
                "PASSWORD_POLICY_VIOLATION",
                "PASSWORD_REUSED",
            ),
            ChangePasswordResultEnum.entries.map { it.name },
        )
    }

    @Test
    fun valueOfAndOrdinal() {
        assertEquals(ChangePasswordResultEnum.SUCCESS, ChangePasswordResultEnum.valueOf("SUCCESS"))
        assertEquals(0, ChangePasswordResultEnum.SUCCESS.ordinal)
        assertEquals(2, ChangePasswordResultEnum.OLD_PASSWORD_WRONG.ordinal)
    }

}
