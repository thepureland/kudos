package io.kudos.ms.auth.common.datascope.vo.request

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleOrgBindRequest
 *
 * Covers the default empty orgIds, full construction, list order preservation, Serializable
 * marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleOrgBindRequestTest {

    @Test
    fun defaultOrgIdsIsEmpty() {
        val req = AuthRoleOrgBindRequest(roleId = "r-1")
        assertEquals("r-1", req.roleId)
        assertTrue(req.orgIds.isEmpty())
    }

    @Test
    fun preservesOrgIdOrder() {
        val ids = listOf("o-3", "o-1", "o-2")
        val req = AuthRoleOrgBindRequest(roleId = "r-1", orgIds = ids)
        assertEquals(listOf("o-3", "o-1", "o-2"), req.orgIds)
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleOrgBindRequest("r-1") is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleOrgBindRequest(roleId = "r-1", orgIds = listOf("o-1"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(orgIds = listOf("o-2")))
        assertNotEquals(a, a.copy(roleId = "r-2"))
        assertTrue(a.toString().contains("r-1"))
    }
}
