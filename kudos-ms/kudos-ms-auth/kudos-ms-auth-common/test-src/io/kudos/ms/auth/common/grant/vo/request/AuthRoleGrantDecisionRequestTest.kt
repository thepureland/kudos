package io.kudos.ms.auth.common.grant.vo.request

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleGrantDecisionRequest
 *
 * Covers default null comment, full construction, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleGrantDecisionRequestTest {

    @Test
    fun defaultCommentIsNull() {
        val req = AuthRoleGrantDecisionRequest(id = "g-1")
        assertEquals("g-1", req.id)
        assertNull(req.comment)
    }

    @Test
    fun withComment() {
        val req = AuthRoleGrantDecisionRequest(id = "g-1", comment = "approved by manager")
        assertEquals("approved by manager", req.comment)
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleGrantDecisionRequest("g-1") is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleGrantDecisionRequest(id = "g-1", comment = "c")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(comment = null))
        assertNotEquals(a, a.copy(id = "g-2"))
        assertTrue(a.toString().contains("g-1"))
    }
}
