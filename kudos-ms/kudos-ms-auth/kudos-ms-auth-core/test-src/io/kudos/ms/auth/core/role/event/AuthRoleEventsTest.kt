package io.kudos.ms.auth.core.role.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for the role domain event value types
 * ([AuthRoleInserted], [AuthRoleUpdated], [AuthRoleDeleted], [AuthRoleBatchDeleted] and its [Item]),
 * plus the join-table relation events ([AuthRoleResourceRelationsChanged], [AuthRoleUserRelationsChanged]).
 *
 * Verifies the data-class contract (equality, hashCode, copy, component destructuring, toString) and
 * the custom derived accessors on AuthRoleBatchDeleted: `id` returns the first item's id, and `ids`
 * projects every item's id. No Spring context or DB is needed — these are plain values.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleEventsTest {

    @Test
    fun authRoleInserted_isAnEventCarryingId() {
        val e = AuthRoleInserted("r1")
        assertEquals("r1", e.id)
        assertTrue(e is AuthRoleEvent)
        // data-class contract
        assertEquals(AuthRoleInserted("r1"), e)
        assertEquals(AuthRoleInserted("r1").hashCode(), e.hashCode())
        assertNotEquals(AuthRoleInserted("r2"), e)
        assertEquals("r1", e.copy().id)
        assertEquals("r9", e.copy(id = "r9").id)
        assertTrue(e.toString().contains("r1"))
    }

    @Test
    fun authRoleUpdated_isAnEventCarryingId() {
        val e = AuthRoleUpdated("u1")
        assertEquals("u1", e.id)
        assertTrue(e is AuthRoleEvent)
        assertEquals(AuthRoleUpdated("u1"), e)
        assertNotEquals(AuthRoleInserted("u1"), e as AuthRoleEvent)
    }

    @Test
    fun authRoleDeleted_carriesSnapshotTenantAndCode() {
        val e = AuthRoleDeleted(id = "d1", tenantId = "t1", code = "ROLE_X")
        assertEquals("d1", e.id)
        assertEquals("t1", e.tenantId)
        assertEquals("ROLE_X", e.code)
        // destructuring follows declaration order
        val (id, tenantId, code) = e
        assertEquals("d1", id)
        assertEquals("t1", tenantId)
        assertEquals("ROLE_X", code)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(code = "ROLE_Y"))
    }

    @Test
    fun authRoleBatchDeleted_idReturnsFirstItemId_idsProjectsAll() {
        val items = listOf(
            AuthRoleBatchDeleted.Item("a", "t1", "C_A"),
            AuthRoleBatchDeleted.Item("b", "t1", "C_B"),
            AuthRoleBatchDeleted.Item("c", "t2", "C_C"),
        )
        val e = AuthRoleBatchDeleted(items)
        assertEquals("a", e.id, "id convenience accessor must return the first item's id")
        assertEquals(listOf("a", "b", "c"), e.ids.toList())
        assertEquals(items, e.items)
        assertTrue(e is AuthRoleEvent)
    }

    @Test
    fun authRoleBatchDeleted_item_dataClassContract() {
        val item = AuthRoleBatchDeleted.Item("a", "t1", "C_A")
        val (id, tenantId, code) = item
        assertEquals("a", id)
        assertEquals("t1", tenantId)
        assertEquals("C_A", code)
        assertEquals(AuthRoleBatchDeleted.Item("a", "t1", "C_A"), item)
        assertNotEquals(AuthRoleBatchDeleted.Item("a", "t1", "C_B"), item)
    }

    @Test
    fun authRoleResourceRelationsChanged_contract() {
        val e = AuthRoleResourceRelationsChanged(roleId = "r1", resourceIds = listOf("res1", "res2"))
        assertEquals("r1", e.roleId)
        assertEquals(listOf("res1", "res2"), e.resourceIds.toList())
        assertTrue(e is AuthRoleResourceEvent)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(roleId = "r2"))
        // empty collection is a legal payload
        assertTrue(AuthRoleResourceRelationsChanged("r1", emptyList()).resourceIds.isEmpty())
    }

    @Test
    fun authRoleUserRelationsChanged_contract() {
        val e = AuthRoleUserRelationsChanged(roleId = "r1", userIds = listOf("u1", "u2"))
        assertEquals("r1", e.roleId)
        assertEquals(listOf("u1", "u2"), e.userIds.toList())
        assertTrue(e is AuthRoleUserEvent)
        val (roleId, userIds) = e
        assertEquals("r1", roleId)
        assertEquals(listOf("u1", "u2"), userIds.toList())
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(userIds = listOf("u3")))
    }
}
