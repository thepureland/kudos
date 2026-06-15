package io.kudos.ms.user.common.passport.vo.response

import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for PassportLoginResult companion factory methods + data-class / serialization contract.
 *
 * Each factory is asserted for its status, message, and which optional fields are populated vs null.
 * The accountFrozen blank/non-blank/null branch and the locked nullable-arg branch are covered explicitly.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportLoginResultTest {

    private fun userInfo() = UserInfoModel(
        id = "u-1",
        username = "alice",
        tenantId = "t-1",
        orgId = "o-1",
        accountTypeDictCode = "1",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        loginTime = LocalDateTime.of(2026, 6, 15, 10, 0),
    )

    @Test
    fun success() {
        val ui = userInfo()
        val r = PassportLoginResult.success(ui)
        assertEquals(PassportLoginStatusEnum.SUCCESS, r.status)
        assertEquals(ui, r.userInfo)
        assertNull(r.loginErrorTimes)
        assertNull(r.message)
    }

    @Test
    fun userNotFound() {
        val r = PassportLoginResult.userNotFound()
        assertEquals(PassportLoginStatusEnum.USER_NOT_FOUND, r.status)
        assertNull(r.userInfo)
        assertEquals("User does not exist", r.message)
    }

    @Test
    fun wrongPassword() {
        val r = PassportLoginResult.wrongPassword(3)
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, r.status)
        assertEquals(3, r.loginErrorTimes)
        assertEquals("Incorrect password", r.message)
        assertNull(r.userInfo)
    }

    @Test
    fun inactive() {
        val r = PassportLoginResult.inactive()
        assertEquals(PassportLoginStatusEnum.INACTIVE, r.status)
        assertEquals("Account is disabled", r.message)
    }

    @Test
    fun lockedWithCount() {
        val r = PassportLoginResult.locked(5)
        assertEquals(PassportLoginStatusEnum.LOCKED, r.status)
        assertEquals(5, r.loginErrorTimes)
        assertEquals("Account is locked due to too many failed login attempts", r.message)
    }

    @Test
    fun lockedWithoutCount_defaultsNull() {
        val r = PassportLoginResult.locked()
        assertEquals(PassportLoginStatusEnum.LOCKED, r.status)
        assertNull(r.loginErrorTimes)
    }

    @Test
    fun otpRequired() {
        val r = PassportLoginResult.otpRequired()
        assertEquals(PassportLoginStatusEnum.OTP_REQUIRED, r.status)
        assertEquals("Please enter the dynamic verification code", r.message)
        assertNull(r.loginErrorTimes)
    }

    @Test
    fun otpWrong() {
        val r = PassportLoginResult.otpWrong(4)
        assertEquals(PassportLoginStatusEnum.OTP_WRONG, r.status)
        assertEquals(4, r.loginErrorTimes)
        assertEquals("Incorrect dynamic verification code", r.message)
    }

    @Test
    fun accountFrozenWithTitle() {
        val r = PassportLoginResult.accountFrozen("Suspicious activity")
        assertEquals(PassportLoginStatusEnum.ACCOUNT_FROZEN, r.status)
        assertEquals("Suspicious activity", r.message)
    }

    @Test
    fun accountFrozenWithBlankTitle_fallsBackToDefault() {
        assertEquals("Account is frozen", PassportLoginResult.accountFrozen("   ").message)
        assertEquals("Account is frozen", PassportLoginResult.accountFrozen("").message)
    }

    @Test
    fun accountFrozenWithNullTitle_fallsBackToDefault() {
        assertEquals("Account is frozen", PassportLoginResult.accountFrozen(null).message)
    }

    @Test
    fun dataClassContractAndSerialization() {
        val a = PassportLoginResult.success(userInfo())
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(a) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(a, restored)
    }

}
