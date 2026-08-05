package io.kudos.ms.auth.common.group.vo.request

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for AuthGroupBatchBindUsersRequest
 *
 * Covers construction, list order preservation, empty lists, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupBatchBindUsersRequestTest {

    @Test
    fun construction() {
        val req = AuthGroupBatchBindUsersRequest(
            groupIds = listOf("g2", "g1"),
            userIds = listOf("u3", "u1", "u2"),
        )
        assertEquals(listOf("g2", "g1"), req.groupIds)
        assertEquals(listOf("u3", "u1", "u2"), req.userIds)
    }

    @Test
    fun emptyLists() {
        val req = AuthGroupBatchBindUsersRequest(groupIds = emptyList(), userIds = emptyList())
        assertTrue(req.groupIds.isEmpty())
        assertTrue(req.userIds.isEmpty())
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthGroupBatchBindUsersRequest(emptyList(), emptyList()) is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupBatchBindUsersRequest(listOf("g1"), listOf("u1"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(userIds = listOf("u2")))
        assertTrue(a.toString().contains("g1"))
    }
}
