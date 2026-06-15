package io.kudos.ms.auth.common.grant.vo.request

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleGrantSubmitRequest
 *
 * Covers default null reason, full construction, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleGrantSubmitRequestTest {

    @Test
    fun defaultReasonIsNull() {
        val req = AuthRoleGrantSubmitRequest(roleId = "r-1", userId = "u-1")
        assertEquals("r-1", req.roleId)
        assertEquals("u-1", req.userId)
        assertNull(req.reason)
    }

    @Test
    fun withReason() {
        val req = AuthRoleGrantSubmitRequest(roleId = "r-1", userId = "u-1", reason = "need access")
        assertEquals("need access", req.reason)
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleGrantSubmitRequest("r-1", "u-1") is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleGrantSubmitRequest(roleId = "r-1", userId = "u-1", reason = "x")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(userId = "u-2"))
        assertTrue(a.toString().contains("r-1"))
    }
}
