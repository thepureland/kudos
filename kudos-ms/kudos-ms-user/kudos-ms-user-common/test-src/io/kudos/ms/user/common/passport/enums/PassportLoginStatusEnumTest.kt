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
        assertEquals(11, PassportLoginStatusEnum.entries.size)
        assertEquals(
            listOf(
                "SUCCESS", "USER_NOT_FOUND", "WRONG_PASSWORD", "INACTIVE",
                "LOCKED", "OTP_REQUIRED", "OTP_WRONG", "RECOVERY_CODE_WRONG", "RATE_LIMITED", "ACCOUNT_FROZEN",
                "INVALID_CREDENTIALS",
            ),
            PassportLoginStatusEnum.entries.map { it.name },
        )
    }

    @Test
    fun valueOf() {
        assertEquals(PassportLoginStatusEnum.OTP_REQUIRED, PassportLoginStatusEnum.valueOf("OTP_REQUIRED"))
        assertEquals(PassportLoginStatusEnum.ACCOUNT_FROZEN, PassportLoginStatusEnum.valueOf("ACCOUNT_FROZEN"))
        assertEquals(PassportLoginStatusEnum.INVALID_CREDENTIALS, PassportLoginStatusEnum.valueOf("INVALID_CREDENTIALS"))
    }

}
