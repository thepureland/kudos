package io.kudos.ms.auth.core.group.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for the group domain event value types in this package:
 * [AuthGroupInserted], [AuthGroupUpdated], [AuthGroupDeleted], [AuthGroupBatchDeleted] (+ its [AuthGroupBatchDeleted.Item]),
 * [AuthGroupRoleRelationsChanged] and [AuthGroupUserRelationsChanged].
 *
 * Verifies the data-class contract (equality, hashCode, copy, destructuring, toString) and the custom
 * derived accessors on [AuthGroupBatchDeleted]: `id` returns the first item's id and `ids` projects
 * every item's id (and that `id` on an empty batch throws, since it delegates to `items.first()`).
 * No Spring context or DB is needed — these are plain values.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupEventsTest {

    @Test
    fun authGroupInserted_carriesId_andDataContract() {
        val e = AuthGroupInserted("g1")
        assertEquals("g1", e.id)
        assertTrue(e is AuthGroupEvent)
        assertEquals(AuthGroupInserted("g1"), e)
        assertEquals(AuthGroupInserted("g1").hashCode(), e.hashCode())
        assertNotEquals(AuthGroupInserted("g2"), e)
        assertEquals("g1", e.copy().id)
        assertEquals("g9", e.copy(id = "g9").id)
        assertTrue(e.toString().contains("g1"))
    }

    @Test
    fun authGroupUpdated_carriesId_andIsDistinctTypeFromInserted() {
        val e = AuthGroupUpdated("u1")
        assertEquals("u1", e.id)
        assertTrue(e is AuthGroupEvent)
        assertEquals(AuthGroupUpdated("u1"), e)
        // Same id but a different event subtype must not be equal.
        assertNotEquals(AuthGroupInserted("u1") as AuthGroupEvent, e as AuthGroupEvent)
    }

    @Test
    fun authGroupDeleted_carriesSnapshotTenantAndCode() {
        val e = AuthGroupDeleted(id = "d1", tenantId = "t1", code = "GRP_X")
        assertEquals("d1", e.id)
        assertEquals("t1", e.tenantId)
        assertEquals("GRP_X", e.code)
        val (id, tenantId, code) = e
        assertEquals("d1", id)
        assertEquals("t1", tenantId)
        assertEquals("GRP_X", code)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(code = "GRP_Y"))
        assertTrue(e is AuthGroupEvent)
    }

    @Test
    fun authGroupBatchDeleted_idReturnsFirstItemId_idsProjectsAll() {
        val items = listOf(
            AuthGroupBatchDeleted.Item("a", "t1", "C_A"),
            AuthGroupBatchDeleted.Item("b", "t1", "C_B"),
            AuthGroupBatchDeleted.Item("c", "t2", "C_C"),
        )
        val e = AuthGroupBatchDeleted(items)
        assertEquals("a", e.id, "id convenience accessor must return the first item's id")
        assertEquals(listOf("a", "b", "c"), e.ids.toList())
        assertEquals(items, e.items)
        assertTrue(e is AuthGroupEvent)
        // data-class contract on the wrapper
        assertEquals(AuthGroupBatchDeleted(items), e)
        assertNotEquals(AuthGroupBatchDeleted(items.take(2)), e)
    }

    @Test
    fun authGroupBatchDeleted_emptyItems_idAccessorThrows() {
        // `id` delegates to items.first(); an empty batch must blow up rather than return a bogus id.
        val e = AuthGroupBatchDeleted(emptyList())
        assertTrue(e.ids.isEmpty())
        assertFailsWith<NoSuchElementException> { e.id }
    }

    @Test
    fun authGroupBatchDeleted_item_dataClassContract() {
        val item = AuthGroupBatchDeleted.Item("a", "t1", "C_A")
        val (id, tenantId, code) = item
        assertEquals("a", id)
        assertEquals("t1", tenantId)
        assertEquals("C_A", code)
        assertEquals(AuthGroupBatchDeleted.Item("a", "t1", "C_A"), item)
        assertNotEquals(AuthGroupBatchDeleted.Item("a", "t1", "C_B"), item)
        assertEquals(item, item.copy())
        assertEquals("z", item.copy(id = "z").id)
    }

    @Test
    fun authGroupRoleRelationsChanged_contract() {
        val e = AuthGroupRoleRelationsChanged(groupId = "g1", roleIds = listOf("r1", "r2"))
        assertEquals("g1", e.groupId)
        assertEquals(listOf("r1", "r2"), e.roleIds.toList())
        assertTrue(e is AuthGroupRoleEvent)
        val (groupId, roleIds) = e
        assertEquals("g1", groupId)
        assertEquals(listOf("r1", "r2"), roleIds.toList())
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(groupId = "g2"))
        // empty collection is a legal payload
        assertTrue(AuthGroupRoleRelationsChanged("g1", emptyList()).roleIds.isEmpty())
    }

    @Test
    fun authGroupUserRelationsChanged_contract() {
        val e = AuthGroupUserRelationsChanged(groupId = "g1", userIds = listOf("u1", "u2"))
        assertEquals("g1", e.groupId)
        assertEquals(listOf("u1", "u2"), e.userIds.toList())
        assertTrue(e is AuthGroupUserEvent)
        val (groupId, userIds) = e
        assertEquals("g1", groupId)
        assertEquals(listOf("u1", "u2"), userIds.toList())
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(userIds = listOf("u3")))
        assertTrue(AuthGroupUserRelationsChanged("g1", emptyList()).userIds.isEmpty())
    }
}
