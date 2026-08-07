package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupInserted
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.service.impl.AuthGroupService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import io.kudos.base.query.Criteria
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
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
 * Pure-logic unit test for [AuthGroupService] (DAO, event publisher and the two relation services
 * mocked with Mockito; no Spring container, no DB).
 *
 * AuthGroup implements IHasBuiltIn, so delete paths are exercised through BaseCrudService's
 * built-in-safe criteria deletes. Covers:
 *  - insert(): delegation to the DAO plus the AuthGroupInserted publication;
 *  - update(): success branch (publishes AuthGroupUpdated) and failure branch (no event, error logged);
 *  - deleteById(): not-found short-circuit (no delete, no event), success (publishes AuthGroupDeleted
 *    carrying the snapshot tenantId/code) and the "delete returned false" branch (no event);
 *  - batchDelete(): empty-input short-circuit (no snapshot query, no event), and the populated path
 *    that snapshots tenantId/code up front and publishes AuthGroupBatchDeleted;
 *  - getDeleteImpact(): empty-input zero, and the de-duplicated union across multiple groups;
 *  - batchBindUsers(): empty-group / empty-user short-circuit, all-success, and partial failure where
 *    one group throws and is surfaced in failures rather than aborting the rest.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class AuthGroupServiceUnitTest {

    private val dao = mock(AuthGroupDao::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val groupUserService = mock(IAuthGroupUserService::class.java)
    private val groupRoleService = mock(IAuthGroupRoleService::class.java)
    private val groupUserDao = mock(AuthGroupUserDao::class.java)
    private val groupRoleDao = mock(AuthGroupRoleDao::class.java)
    private val tenantAdministrationGuard = mock(TenantAdministrationGuard::class.java)

    private val service = AuthGroupService(dao).apply {
        inject("eventPublisher", eventPublisher)
        inject("authGroupUserService", groupUserService)
        inject("authGroupRoleService", groupRoleService)
        inject("authGroupUserDao", groupUserDao)
        inject("authGroupRoleDao", groupRoleDao)
        inject("tenantAdministrationGuard", tenantAdministrationGuard)
    }

    private fun AuthGroupService.inject(field: String, value: Any) {
        val f = AuthGroupService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    private fun anyGroup(): AuthGroup = ArgumentMatchers.any(AuthGroup::class.java) ?: group("x")

    private fun anyCriteria(): Criteria = ArgumentMatchers.any(Criteria::class.java) ?: Criteria()

    private fun anyBuiltInProperty() =
        ArgumentMatchers.same(AuthGroup::builtIn) ?: AuthGroup::builtIn

    private fun group(id: String, tenantId: String = "t1", code: String = "GRP"): AuthGroup =
        AuthGroup {
            this.id = id
            this.tenantId = tenantId
            this.subsysCode = "ams"
            this.code = code
            this.active = true
            this.builtIn = false
        }

    // ---------------------------------------------------------------- insert

    @Test
    fun insert_delegatesToDao_andPublishesInsertedEvent() {
        val po = group("g1")
        whenCalled(dao.insert(po)).thenReturn("g1")
        val id = service.insert(po)
        assertEquals("g1", id)
        verify(dao).insert(po)
        verify(eventPublisher).publishEvent(AuthGroupInserted("g1"))
    }

    // ---------------------------------------------------------------- update

    @Test
    fun update_success_publishesUpdatedEvent() {
        val po = group("g2")
        whenCalled(dao.get("g2")).thenReturn(po)
        whenCalled(dao.update(po)).thenReturn(true)
        assertTrue(service.update(po))
        verify(eventPublisher).publishEvent(AuthGroupUpdated("g2"))
    }

    @Test
    fun update_failure_doesNotPublishEvent() {
        val po = group("g3")
        whenCalled(dao.get("g3")).thenReturn(po)
        whenCalled(dao.update(po)).thenReturn(false)
        assertFalse(service.update(po))
        verify(eventPublisher, never()).publishEvent(AuthGroupUpdated("g3"))
    }

    // ---------------------------------------------------------------- deleteById

    @Test
    fun deleteById_missing_shortCircuitsWithoutDeleteOrEvent() {
        whenCalled(dao.get("ghost")).thenReturn(null)
        assertFalse(service.deleteById("ghost"))
        verify(dao, never()).deleteById(anyString())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun deleteById_success_publishesDeletedEventWithSnapshot() {
        whenCalled(dao.get("g4")).thenReturn(group("g4", tenantId = "tenantA", code = "CODE_A"))
        whenCalled(groupUserDao.searchMemberUserIdsByGroupId("g4")).thenReturn(emptySet())
        whenCalled(groupRoleDao.searchRoleIdsByGroupId("g4")).thenReturn(emptySet())
        whenCalled(dao.batchDeleteCriteria(anyCriteria())).thenReturn(1)
        assertTrue(service.deleteById("g4"))
        verify(eventPublisher).publishEvent(AuthGroupDeleted("g4", "tenantA", "CODE_A"))
    }

    @Test
    fun deleteById_daoReturnsFalse_doesNotPublishEvent() {
        whenCalled(dao.get("g5")).thenReturn(group("g5", tenantId = "tA", code = "C"))
        whenCalled(groupUserDao.searchMemberUserIdsByGroupId("g5")).thenReturn(emptySet())
        whenCalled(groupRoleDao.searchRoleIdsByGroupId("g5")).thenReturn(emptySet())
        whenCalled(dao.batchDeleteCriteria(anyCriteria())).thenReturn(0)
        whenCalled(dao.searchProperty(anyCriteria(), anyBuiltInProperty())).thenReturn(emptyList())
        assertFalse(service.deleteById("g5"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    // ---------------------------------------------------------------- batchDelete

    @Test
    fun batchDelete_empty_skipsSnapshotDeleteAndEvent() {
        val count = service.batchDelete(emptyList())
        assertEquals(0, count)
        verify(dao, never()).getByIds(ArgumentMatchers.anyList(), ArgumentMatchers.anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchDelete_populated_snapshotsThenPublishesBatchDeletedEvent() {
        val ids = listOf("g6", "g7")
        whenCalled(dao.getByIds(ids)).thenReturn(
            listOf(
                group("g6", tenantId = "t1", code = "C6"),
                group("g7", tenantId = "t2", code = "C7"),
            )
        )
        ids.forEach {
            whenCalled(groupUserDao.searchMemberUserIdsByGroupId(it)).thenReturn(emptySet())
            whenCalled(groupRoleDao.searchRoleIdsByGroupId(it)).thenReturn(emptySet())
        }
        whenCalled(dao.batchDeleteCriteria(anyCriteria())).thenReturn(2)
        val count = service.batchDelete(ids)
        assertEquals(2, count)
        val captor = ArgumentCaptor.forClass(AuthGroupBatchDeleted::class.java)
        verify(eventPublisher).publishEvent(captor.capture() ?: AuthGroupBatchDeleted(emptyList()))
        val items = captor.value.items.toList()
        assertEquals(listOf("g6", "g7"), items.map { it.id })
        assertEquals(listOf("C6", "C7"), items.map { it.code })
        assertEquals(listOf("t1", "t2"), items.map { it.tenantId })
    }

    @Test
    fun batchDelete_populatedButSnapshotEmpty_skipsEvent() {
        // ids non-empty but the DB returns no rows (already gone): no batch-deleted event published.
        val ids = listOf("gone")
        whenCalled(dao.getByIds(ids)).thenReturn(emptyList())
        whenCalled(dao.batchDeleteCriteria(anyCriteria())).thenReturn(0)
        whenCalled(dao.inSearchPropertiesById(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.anyList(),
        )).thenReturn(emptyList())
        val count = service.batchDelete(ids)
        assertEquals(0, count)
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    // ---------------------------------------------------------------- getDeleteImpact

    @Test
    fun getDeleteImpact_empty_returnsZero() {
        val vo = service.getDeleteImpact(emptyList())
        assertEquals(0, vo.users)
        assertEquals(0, vo.roles)
        verify(groupUserService, never()).getUserIdsByGroupId(anyString())
    }

    @Test
    fun getDeleteImpact_unionsAcrossGroups_countingSharedMembersOnce() {
        whenCalled(groupUserService.getUserIdsByGroupId("g1")).thenReturn(setOf("u1", "u2"))
        whenCalled(groupUserService.getUserIdsByGroupId("g2")).thenReturn(setOf("u2", "u3"))
        whenCalled(groupRoleService.getRoleIdsByGroupId("g1")).thenReturn(setOf("r1"))
        whenCalled(groupRoleService.getRoleIdsByGroupId("g2")).thenReturn(setOf("r1", "r2"))
        val vo = service.getDeleteImpact(listOf("g1", "g2"))
        // union of users {u1,u2,u3} = 3, union of roles {r1,r2} = 2
        assertEquals(3, vo.users)
        assertEquals(2, vo.roles)
    }

    // ---------------------------------------------------------------- batchBindUsers

    @Test
    fun batchBindUsers_emptyGroups_returnsEmpty() {
        val r = service.batchBindUsers(emptyList(), listOf("u1"))
        assertEquals(0, r.ok)
        assertTrue(r.failures.isEmpty())
        verify(groupUserService, never()).batchBind(anyString(), ArgumentMatchers.anyList())
    }

    @Test
    fun batchBindUsers_emptyUsers_returnsEmpty() {
        val r = service.batchBindUsers(listOf("g1"), emptyList())
        assertEquals(0, r.ok)
        assertTrue(r.failures.isEmpty())
        verify(groupUserService, never()).batchBind(anyString(), ArgumentMatchers.anyList())
    }

    @Test
    fun batchBindUsers_allSucceed() {
        val userIds = listOf("u1", "u2")
        whenCalled(groupUserService.batchBind(anyString(), ArgumentMatchers.anyList())).thenReturn(2)
        val r = service.batchBindUsers(listOf("g1", "g2"), userIds)
        assertEquals(2, r.ok)
        assertTrue(r.failures.isEmpty())
        verify(groupUserService).batchBind("g1", userIds)
        verify(groupUserService).batchBind("g2", userIds)
    }

    @Test
    fun batchBindUsers_partialFailure_surfacedInFailures_othersContinue() {
        val userIds = listOf("u1")
        whenCalled(groupUserService.batchBind("gOk", userIds)).thenReturn(1)
        whenCalled(groupUserService.batchBind("gBad", userIds))
            .thenThrow(IllegalStateException("boom"))
        val r = service.batchBindUsers(listOf("gOk", "gBad"), userIds)
        assertEquals(1, r.ok)
        assertEquals(1, r.failures.size)
        assertEquals("gBad", r.failures.single().ownerId)
        assertEquals("boom", r.failures.single().reason)
    }

    @Test
    fun batchBindUsers_failureWithNullMessage_fallsBackToExceptionClassName() {
        val userIds = listOf("u1")
        // An exception whose message is null must fall back to the simple class name as the reason.
        whenCalled(groupUserService.batchBind("gNull", userIds))
            .thenThrow(RuntimeException())
        val r = service.batchBindUsers(listOf("gNull"), userIds)
        assertEquals(0, r.ok)
        assertEquals("RuntimeException", r.failures.single().reason)
    }
}
