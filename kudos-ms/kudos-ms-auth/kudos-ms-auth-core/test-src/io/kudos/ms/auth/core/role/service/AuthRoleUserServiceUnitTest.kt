package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.impl.AuthRoleUserService
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.GrantRejection
import io.kudos.ms.auth.core.version.dao.AuthPrincipalVersionDao
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthRoleUserService] (DAO/cache/event/policy collaborators mocked
 * with Mockito; no Spring container and no DB) — complements the container-backed
 * [AuthRoleUserServiceTest] by exercising the bind/unbind bookkeeping deterministically.
 *
 * Admission is split between two collaborators, and both are mocked here: role existence,
 * separation-of-duties and the effective-role expansion those run against live in
 * [IAuthGrantPolicyService], while tenant scope stays in this class, in the private
 * `assertCanManageRole` guard every public path opens with. What is asserted is therefore that the
 * service consults them and honours their verdict — plus the bookkeeping around that:
 *
 *  - [AuthRoleUserService.batchBind]: empty-input short-circuit (the policy is never consulted); a
 *    policy rejection aborting the whole batch, both when the role does not exist and when a
 *    candidate trips an SoD exclusion (all-or-nothing: no insert, no event); the all-already-bound
 *    short-circuit (candidates empty ⇒ no insert/event); and a happy path that inserts only the
 *    not-yet-bound users and publishes one change event.
 *  - [AuthRoleUserService.unbind]: success (delete>0 ⇒ event) and no-op (delete==0 ⇒ false, no event).
 *  - [AuthRoleUserService.exists] / getUserIdsByRoleId / getRoleIdsByUserId: delegation + Set conv.
 *
 * The DAO is supplied through the [io.kudos.base.support.service.impl.BaseCrudService] constructor;
 * the `@Autowired`/`@Resource lateinit var` collaborators are injected by reflection. The
 * [TenantAdministrationGuard] mock permits throughout and `authRoleDao.get` always resolves, so the
 * guard is only kept off the path here; a refusal by it is not covered yet.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleUserServiceUnitTest {

    private val dao = mock(AuthRoleUserDao::class.java)
    private val userIdsByRoleIdCache = mock(UserIdsByRoleIdCache::class.java)
    private val roleIdsByUserIdCache = mock(RoleIdsByUserIdCache::class.java)
    private val resourceIdsByUserIdCache = mock(ResourceIdsByUserIdCache::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val grantPolicyService = mock(IAuthGrantPolicyService::class.java)
    private val authPrincipalVersionDao = mock(AuthPrincipalVersionDao::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val tenantAdministrationGuard = mock(TenantAdministrationGuard::class.java)

    private val service = AuthRoleUserService(dao).apply {
        inject("userIdsByRoleIdCache", userIdsByRoleIdCache)
        inject("roleIdsByUserIdCache", roleIdsByUserIdCache)
        inject("resourceIdsByUserIdCache", resourceIdsByUserIdCache)
        inject("grantPolicyService", grantPolicyService)
        inject("authPrincipalVersionDao", authPrincipalVersionDao)
        inject("authRoleDao", authRoleDao)
        inject("tenantAdministrationGuard", tenantAdministrationGuard)
        inject("eventPublisher", eventPublisher)
    }

    init {
        whenCalled(authRoleDao.get(ArgumentMatchers.anyString())).thenReturn(role())
    }

    private fun AuthRoleUserService.inject(field: String, value: Any) {
        val f = AuthRoleUserService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationCollection(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    // ---------------------------------------------------------------- batchBind guards

    @Test
    fun batchBind_emptyInput_returnsZero() {
        assertEquals(0, service.batchBind("role1", emptyList()))
        verify(grantPolicyService, never()).screenGrants(anyCandidates())
    }

    /**
     * Admission — role existence included — now lives in [IAuthGrantPolicyService], the single gate
     * every one of the write paths reduces to. What this class is responsible for is *consulting*
     * it and honouring its verdict, so that is what is asserted.
     */
    @Test
    fun batchBind_rejectedByPolicy_abortsWholeBatch() {
        whenCalled(grantPolicyService.assertNoRejection(anyRejections()))
            .thenThrow(IllegalArgumentException("role does not exist"))
        val err = assertFailsWith<IllegalArgumentException> { service.batchBind("ghost", listOf("u1")) }
        assertTrue(err.message!!.contains("role does not exist"))
        verify(dao, never()).batchInsert(anyRelationCollection(), ArgumentMatchers.anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_allAlreadyBound_returnsZeroNoInsertNoEvent() {
        whenCalled(dao.searchUserIdsByRoleId("role1")).thenReturn(listOf("u1", "u2"))
        assertEquals(0, service.batchBind("role1", listOf("u1", "u2")))
        verify(dao, never()).batchInsert(anyRelationCollection(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    // ---------------------------------------------------------------- batchBind happy path

    @Test
    fun batchBind_newUsers_insertsOnlyMissingAndPublishesEvent() {
        whenCalled(dao.searchUserIdsByRoleId("role1")).thenReturn(listOf("u1")) // u1 already bound
        // Admission is the policy service's job now; here it raises nothing.
        whenCalled(grantPolicyService.screenGrants(anyCandidates())).thenReturn(emptyList())
        whenCalled(dao.batchInsert(anyRelationCollection(), anyInt())).thenReturn(2)

        val bound = service.batchBind("role1", listOf("u1", "u2", "u3"))
        assertEquals(2, bound)

        @Suppress("UNCHECKED_CAST")
        val relCaptor = ArgumentCaptor.forClass(Collection::class.java) as ArgumentCaptor<Collection<Any>>
        verify(dao).batchInsert(relCaptor.capture() ?: emptyList(), anyInt())
        val inserted = relCaptor.value.map { it as AuthRoleUser }
        assertEquals(setOf("u2", "u3"), inserted.mapTo(mutableSetOf()) { it.userId })
        assertTrue(inserted.all { it.roleId == "role1" })

        val evtCaptor = ArgumentCaptor.forClass(AuthRoleUserRelationsChanged::class.java)
        verify(eventPublisher).publishEvent(evtCaptor.capture() ?: AuthRoleUserRelationsChanged("x", emptyList()))
        assertEquals("role1", evtCaptor.value.roleId)
        assertEquals(setOf("u2", "u3"), evtCaptor.value.userIds.toSet())
    }

    @Test
    fun batchBind_sodViolation_rejectsWholeBatchNoInsert() {
        whenCalled(dao.searchUserIdsByRoleId("role1")).thenReturn(emptyList())
        // A direct bind is all-or-nothing: one rejected candidate aborts the whole call.
        whenCalled(grantPolicyService.assertNoRejection(anyRejections()))
            .thenThrow(IllegalArgumentException("separation-of-duties: conflicts with held ↔ role1 (exclusion ex1)"))

        val err = assertFailsWith<IllegalArgumentException> { service.batchBind("role1", listOf("u2")) }
        assertTrue(err.message!!.contains("separation-of-duties"))
        assertTrue(err.message!!.contains("ex1"))
        verify(dao, never()).batchInsert(anyRelationCollection(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    // ---------------------------------------------------------------- unbind

    @Test
    fun unbind_deleted_publishesEvent() {
        // The cascade reads the subtree under the principal's lock before the row goes.
        whenCalled(dao.searchGrantsByRoleId("role1")).thenReturn(emptyList())
        whenCalled(dao.deleteByRoleIdAndUserId("role1", "u1")).thenReturn(1)
        assertTrue(service.unbind("role1", "u1"))
        val captor = ArgumentCaptor.forClass(AuthRoleUserRelationsChanged::class.java)
        verify(eventPublisher).publishEvent(captor.capture() ?: AuthRoleUserRelationsChanged("x", emptyList()))
        assertEquals(listOf("u1"), captor.value.userIds.toList())
    }

    @Test
    fun unbind_nothingDeleted_returnsFalseNoEvent() {
        whenCalled(dao.searchGrantsByRoleId("role1")).thenReturn(emptyList())
        whenCalled(dao.deleteByRoleIdAndUserId("role1", "ghost")).thenReturn(0)
        assertFalse(service.unbind("role1", "ghost"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    // ---------------------------------------------------------------- exists / getters

    @Test
    fun exists_delegatesToDao() {
        whenCalled(dao.exists("role1", "u1")).thenReturn(true)
        assertTrue(service.exists("role1", "u1"))
    }

    @Test
    fun getUserIdsByRoleId_delegatesToCacheAsSet() {
        whenCalled(userIdsByRoleIdCache.getUserIds("role1")).thenReturn(listOf("u1", "u2", "u1"))
        assertEquals(setOf("u1", "u2"), service.getUserIdsByRoleId("role1"))
    }

    @Test
    fun getRoleIdsByUserId_delegatesToCacheAsSet() {
        whenCalled(roleIdsByUserIdCache.getRoleIds("u1")).thenReturn(listOf("r1", "r2"))
        assertEquals(setOf("r1", "r2"), service.getRoleIdsByUserId("u1"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyCandidates(): Collection<GrantCandidate> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<GrantCandidate>?) ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun anyRejections(): List<GrantRejection> =
        (ArgumentMatchers.any(List::class.java) as List<GrantRejection>?) ?: emptyList()

    private fun role() = AuthRole {
        id = "role1"
        tenantId = "t1"
        subsysCode = "ams"
        code = "ROLE"
        active = true
        builtIn = false
    }
}
