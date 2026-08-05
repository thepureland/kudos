package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.service.impl.AuthGroupRoleService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthGroupRoleService] (DAO and event publisher mocked; no Spring, no DB).
 *
 * Read-throughs ([getRoleIdsByGroupId], [getGroupIdsByRoleId], [exists]) are verified to delegate to
 * the DAO untouched. The write paths exercise the de-duplication and event semantics:
 *  - batchBind(): empty input → 0 with no DAO/event activity; all-already-bound → 0, no insert, no event;
 *    a delta → inserts only the new relations and publishes AuthGroupRoleRelationsChanged carrying
 *    exactly the newly-inserted role ids;
 *  - unbind(): success (publishes the change event) and the not-found branch (count 0 → false, no event).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupRoleServiceUnitTest {

    private val dao = mock(AuthGroupRoleDao::class.java)
    private val authGroupUserDao = mock(AuthGroupUserDao::class.java)
    private val grantPolicyService = mock(IAuthGrantPolicyService::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)

    private val service = AuthGroupRoleService(dao).apply {
        inject("eventPublisher", eventPublisher)
        // Binding a role to a group hands it to every member, so it is screened like a direct grant.
        inject("authGroupUserDao", authGroupUserDao)
        inject("grantPolicyService", grantPolicyService)
    }

    private fun AuthGroupRoleService.inject(field: String, value: Any) {
        val f = AuthGroupRoleService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationList(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    // ---------------------------------------------------------------- read-throughs

    @Test
    fun getRoleIdsByGroupId_delegates() {
        whenCalled(dao.searchRoleIdsByGroupId("g1")).thenReturn(setOf("r1", "r2"))
        assertEquals(setOf("r1", "r2"), service.getRoleIdsByGroupId("g1"))
    }

    @Test
    fun getGroupIdsByRoleId_delegates() {
        whenCalled(dao.searchGroupIdsByRoleId("r1")).thenReturn(setOf("g1"))
        assertEquals(setOf("g1"), service.getGroupIdsByRoleId("r1"))
    }

    @Test
    fun exists_delegates() {
        whenCalled(dao.exists("g1", "r1")).thenReturn(true)
        assertTrue(service.exists("g1", "r1"))
        whenCalled(dao.exists("g1", "rX")).thenReturn(false)
        assertFalse(service.exists("g1", "rX"))
    }

    // ---------------------------------------------------------------- batchBind

    @Test
    fun batchBind_empty_returnsZero_noDaoNoEvent() {
        assertEquals(0, service.batchBind("g1", emptyList()))
        verify(dao, never()).searchRoleIdsByGroupId(anyString())
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_allAlreadyExist_returnsZero_noInsertNoEvent() {
        whenCalled(dao.searchRoleIdsByGroupId("g1")).thenReturn(setOf("r1", "r2"))
        assertEquals(0, service.batchBind("g1", listOf("r1", "r2")))
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_insertsOnlyDelta_andPublishesChangeWithNewIds() {
        whenCalled(dao.searchRoleIdsByGroupId("g1")).thenReturn(setOf("r1"))
        // r1 already exists; only r2 and r3 are new.
        val count = service.batchBind("g1", listOf("r1", "r2", "r3"))
        assertEquals(2, count)

        val insertCaptor = ArgumentCaptor.forClass(Collection::class.java)
        @Suppress("UNCHECKED_CAST")
        verify(dao).batchInsert((insertCaptor.capture() ?: emptyList<AuthGroupRole>()) as Collection<Any>, anyInt())
        @Suppress("UNCHECKED_CAST")
        val inserted = insertCaptor.value as Collection<AuthGroupRole>
        assertEquals(setOf("r2", "r3"), inserted.map { it.roleId }.toSet())
        assertTrue(inserted.all { it.groupId == "g1" })

        val eventCaptor = ArgumentCaptor.forClass(AuthGroupRoleRelationsChanged::class.java)
        verify(eventPublisher).publishEvent(
            eventCaptor.capture() ?: AuthGroupRoleRelationsChanged("g1", emptyList())
        )
        assertEquals("g1", eventCaptor.value.groupId)
        assertEquals(setOf("r2", "r3"), eventCaptor.value.roleIds.toSet())
    }

    // ---------------------------------------------------------------- unbind

    @Test
    fun unbind_success_publishesChange() {
        whenCalled(dao.deleteByGroupIdAndRoleId("g1", "r1")).thenReturn(1)
        assertTrue(service.unbind("g1", "r1"))
        verify(eventPublisher).publishEvent(AuthGroupRoleRelationsChanged("g1", listOf("r1")))
    }

    @Test
    fun unbind_relationMissing_returnsFalse_noEvent() {
        whenCalled(dao.deleteByGroupIdAndRoleId("g1", "rX")).thenReturn(0)
        assertFalse(service.unbind("g1", "rX"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }
}
