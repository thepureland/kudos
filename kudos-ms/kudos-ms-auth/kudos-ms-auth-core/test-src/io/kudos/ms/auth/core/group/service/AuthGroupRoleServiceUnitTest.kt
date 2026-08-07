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
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.model.po.AuthRole
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
 * @author AI: Codex
 * @since 1.0.0
 */
internal class AuthGroupRoleServiceUnitTest {

    private val dao = mock(AuthGroupRoleDao::class.java)
    private val authGroupUserDao = mock(AuthGroupUserDao::class.java)
    private val grantPolicyService = mock(IAuthGrantPolicyService::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val authGroupDao = mock(AuthGroupDao::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val tenantGuard = mock(TenantAdministrationGuard::class.java)

    private val service = AuthGroupRoleService(dao).apply {
        inject("eventPublisher", eventPublisher)
        // Binding a role to a group hands it to every member, so it is screened like a direct grant.
        inject("authGroupUserDao", authGroupUserDao)
        inject("grantPolicyService", grantPolicyService)
        inject("authGroupDao", authGroupDao)
        inject("authRoleDao", authRoleDao)
        inject("tenantAdministrationGuard", tenantGuard)
    }

    init {
        whenCalled(authGroupDao.get(anyString())).thenAnswer { group(it.getArgument(0)) }
        whenCalled(authRoleDao.get(anyString())).thenAnswer { role(it.getArgument(0)) }
        whenCalled(authRoleDao.getByIds(anyStringCollection(), anyInt())).thenAnswer { invocation ->
            invocation.getArgument<Collection<String>>(0).map(::role)
        }
    }

    private fun AuthGroupRoleService.inject(field: String, value: Any) {
        val f = AuthGroupRoleService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationList(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun anyStringCollection(): Collection<String> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<String>?) ?: emptyList()

    private fun group(id: String) = AuthGroup {
        this.id = id; tenantId = "t1"; subsysCode = "ams"; active = true; builtIn = false
    }

    private fun role(id: String) = AuthRole {
        this.id = id; tenantId = "t1"; subsysCode = "ams"; active = true; builtIn = false
    }

    private fun binding(roleId: String, revoked: Boolean = false) = AuthGroupRole {
        id = "binding-$roleId"; groupId = "g1"; this.roleId = roleId; this.revoked = revoked
    }

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
        whenCalled(dao.searchRoleIdsByGroupId("g1")).thenReturn(setOf("r1"))
        assertTrue(service.exists("g1", "r1"))
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
        whenCalled(dao.searchBindingsByGroupId("g1")).thenReturn(listOf(binding("r1"), binding("r2")))
        assertEquals(0, service.batchBind("g1", listOf("r1", "r2")))
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_insertsOnlyDelta_andPublishesChangeWithNewIds() {
        whenCalled(dao.searchBindingsByGroupId("g1")).thenReturn(listOf(binding("r1")))
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
        whenCalled(dao.searchBindingsByGroupId("g1")).thenReturn(listOf(binding("r1")))
        assertTrue(service.unbind("g1", "r1"))
        verify(eventPublisher).publishEvent(AuthGroupRoleRelationsChanged("g1", listOf("r1")))
    }

    @Test
    fun unbind_relationMissing_returnsFalse_noEvent() {
        whenCalled(dao.searchBindingsByGroupId("g1")).thenReturn(emptyList())
        assertFalse(service.unbind("g1", "rX"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }
}
