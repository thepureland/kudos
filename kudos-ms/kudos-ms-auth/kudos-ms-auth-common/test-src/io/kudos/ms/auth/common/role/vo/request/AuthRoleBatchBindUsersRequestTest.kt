package io.kudos.ms.auth.common.role.vo.request

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleBatchBindUsersRequest
 *
 * Covers construction, list order preservation, empty lists, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleBatchBindUsersRequestTest {

    @Test
    fun construction() {
        val req = AuthRoleBatchBindUsersRequest(roleIds = listOf("r2", "r1"), userIds = listOf("u3", "u1"))
        assertEquals(listOf("r2", "r1"), req.roleIds)
        assertEquals(listOf("u3", "u1"), req.userIds)
    }

    @Test
    fun emptyLists() {
        val req = AuthRoleBatchBindUsersRequest(roleIds = emptyList(), userIds = emptyList())
        assertTrue(req.roleIds.isEmpty())
        assertTrue(req.userIds.isEmpty())
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleBatchBindUsersRequest(emptyList(), emptyList()) is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleBatchBindUsersRequest(listOf("r1"), listOf("u1"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(roleIds = listOf("r2")))
        assertTrue(a.toString().contains("r1"))
    }
}
