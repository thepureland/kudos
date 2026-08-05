package io.kudos.ms.auth.common.role.vo.request

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleCopyRequest
 *
 * Covers the default copyResources = true, explicit false, full construction, Serializable marker
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleCopyRequestTest {

    @Test
    fun defaultCopyResourcesIsTrue() {
        val req = AuthRoleCopyRequest(sourceId = "src-1", code = "NEW", name = "New Role")
        assertEquals("src-1", req.sourceId)
        assertEquals("NEW", req.code)
        assertEquals("New Role", req.name)
        assertTrue(req.copyResources)
    }

    @Test
    fun explicitCopyResourcesFalse() {
        val req = AuthRoleCopyRequest(sourceId = "src-1", code = "NEW", name = "New Role", copyResources = false)
        assertFalse(req.copyResources)
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleCopyRequest("src-1", "NEW", "New Role") is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleCopyRequest(sourceId = "src-1", code = "NEW", name = "New Role")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(copyResources = false))
        assertNotEquals(a, a.copy(code = "OTHER"))
        assertTrue(a.toString().contains("src-1"))
    }
}
