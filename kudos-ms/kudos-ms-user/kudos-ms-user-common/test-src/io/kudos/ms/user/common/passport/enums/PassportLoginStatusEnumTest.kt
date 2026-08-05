package io.kudos.ms.user.common.passport.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for PassportLoginStatusEnum
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportLoginStatusEnumTest {

    @Test
    fun entriesAndOrder() {
        assertEquals(8, PassportLoginStatusEnum.entries.size)
        assertEquals(
            listOf(
                "SUCCESS", "USER_NOT_FOUND", "WRONG_PASSWORD", "INACTIVE",
                "LOCKED", "OTP_REQUIRED", "OTP_WRONG", "ACCOUNT_FROZEN",
            ),
            PassportLoginStatusEnum.entries.map { it.name },
        )
    }

    @Test
    fun valueOf() {
        assertEquals(PassportLoginStatusEnum.OTP_REQUIRED, PassportLoginStatusEnum.valueOf("OTP_REQUIRED"))
        assertEquals(PassportLoginStatusEnum.ACCOUNT_FROZEN, PassportLoginStatusEnum.valueOf("ACCOUNT_FROZEN"))
    }

}
