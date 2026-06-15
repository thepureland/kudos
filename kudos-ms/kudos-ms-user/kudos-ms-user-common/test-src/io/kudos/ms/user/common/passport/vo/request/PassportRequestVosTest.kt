package io.kudos.ms.user.common.passport.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for the passport request VOs: PassportLoginRequest, ChangePasswordRequest, VerifyPasswordRequest.
 *
 * Covers property accessors, optional default values (loginIp / authCode default to null) and
 * the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportRequestVosTest {

    @Test
    fun passportLoginRequestDefaults() {
        val req = PassportLoginRequest(tenantId = "t-1", username = "alice", plainPassword = "pw")
        assertEquals("t-1", req.tenantId)
        assertEquals("alice", req.username)
        assertEquals("pw", req.plainPassword)
        assertNull(req.loginIp)
        assertNull(req.authCode)
    }

    @Test
    fun passportLoginRequestFullArgs() {
        val req = PassportLoginRequest(
            tenantId = "t-1",
            username = "alice",
            plainPassword = "pw",
            loginIp = 16909060L,
            authCode = 123456L,
        )
        assertEquals(16909060L, req.loginIp)
        assertEquals(123456L, req.authCode)
    }

    @Test
    fun passportLoginRequestDataClassContract() {
        val a = PassportLoginRequest("t-1", "alice", "pw")
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(authCode = 1L))
        assertTrue(a.toString().contains("alice"))
    }

    @Test
    fun changePasswordRequest() {
        val a = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "old", newPlainPassword = "new")
        assertEquals("u-1", a.userId)
        assertEquals("old", a.oldPlainPassword)
        assertEquals("new", a.newPlainPassword)
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(newPlainPassword = "x"))
        assertTrue(a.toString().contains("u-1"))
    }

    @Test
    fun verifyPasswordRequest() {
        val a = VerifyPasswordRequest(userId = "u-1", plainPassword = "pw")
        assertEquals("u-1", a.userId)
        assertEquals("pw", a.plainPassword)
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(plainPassword = "other"))
        assertTrue(a.toString().contains("u-1"))
    }

}
