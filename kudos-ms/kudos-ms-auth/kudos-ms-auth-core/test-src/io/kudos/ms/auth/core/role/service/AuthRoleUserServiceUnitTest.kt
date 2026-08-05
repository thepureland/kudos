package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.impl.AuthRoleUserService
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
 * Pure-logic unit test for [AuthRoleUserService] (DAO/cache/event/exclusion collaborators mocked
 * with Mockito; no Spring container and no DB) — complements the container-backed
 * [AuthRoleUserServiceTest] by exercising the SoD-aware bind logic deterministically:
 *
 *  - [AuthRoleUserService.batchBind]: empty-input short-circuit; the role-not-found guard; the
 *    all-already-bound short-circuit (candidates empty ⇒ no insert/event); a happy path that
 *    inserts only the not-yet-bound users and
 *    publishes one change event; and the violation path where one candidate's effective role set
 *    conflicts (whole batch rejected with a message naming the user and the conflicting pair).
 *  - [AuthRoleUserService.findSodViolationMessage] (internal): null when allowed, formatted message
 *    when the exclusion service reports a violation; and the effective-role-set expansion fed to the
 *    exclusion service (direct + group-derived + ancestor chain, de-duplicated).
 *  - [AuthRoleUserService.unbind]: success (delete>0 ⇒ event) and no-op (delete==0 ⇒ false, no event).
 *  - [AuthRoleUserService.exists] / getUserIdsByRoleId / getRoleIdsByUserId: delegation + Set conv.
 *
 * The DAO is supplied through the [io.kudos.base.support.service.impl.BaseCrudService] constructor;
 * the `@Autowired`/`@Resource lateinit var` collaborators are injected by reflection.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleUserServiceUnitTest {

    private val dao = mock(AuthRoleUserDao::class.java)
    private val userIdsByRoleIdCache = mock(UserIdsByRoleIdCache::class.java)
    private val roleIdsByUserIdCache = mock(RoleIdsByUserIdCache::class.java)
    private val resourceIdsByUserIdCache = mock(ResourceIdsByUserIdCache::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val authGroupUserDao = mock(AuthGroupUserDao::class.java)
    private val authGroupRoleDao = mock(AuthGroupRoleDao::class.java)
    private val exclusionService = mock(IAuthRoleExclusionService::class.java)
    private val grantPolicyService = mock(IAuthGrantPolicyService::class.java)
    private val authPrincipalVersionDao = mock(AuthPrincipalVersionDao::class.java)

    private val service = AuthRoleUserService(dao).apply {
        inject("userIdsByRoleIdCache", userIdsByRoleIdCache)
        inject("roleIdsByUserIdCache", roleIdsByUserIdCache)
        inject("resourceIdsByUserIdCache", resourceIdsByUserIdCache)
        inject("grantPolicyService", grantPolicyService)
        inject("authPrincipalVersionDao", authPrincipalVersionDao)
        inject("eventPublisher", eventPublisher)
        inject("authRoleDao", authRoleDao)
        inject("authGroupUserDao", authGroupUserDao)
        inject("authGroupRoleDao", authGroupRoleDao)
        inject("exclusionService", exclusionService)
    }

    private fun AuthRoleUserService.inject(field: String, value: Any) {
        val f = AuthRoleUserService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationCollection(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    // dao.searchRoleIdsByUserId has a `now: LocalDateTime = now()` default arg that the service
    // fills implicitly, so an exact-value stub never matches (the two `now()` instants differ).
    // Stub both positions with matchers; the time matcher coalesces the null Mockito returns.
    private fun anyLdt(): java.time.LocalDateTime =
        ArgumentMatchers.any(java.time.LocalDateTime::class.java) ?: java.time.LocalDateTime.now()

    private fun stubSearchRoleIdsByUserId(roleIds: List<String>) {
        whenCalled(dao.searchRoleIdsByUserId(ArgumentMatchers.anyString(), anyLdt())).thenReturn(roleIds)
    }

    private fun role(id: String, tenant: String?) = AuthRole {
        this.id = id
        if (tenant != null) this.tenantId = tenant
    }

    private fun excl(a: String, b: String, id: String) = AuthRoleExclusion {
        this.roleAId = a
        this.roleBId = b
        this.tenantId = "t1"
        this.id = id
    }

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
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1", "t1"))
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

    // ---------------------------------------------------------------- findSodViolationMessage

    @Test
    fun findSodViolationMessage_allowed_returnsNull() {
        stubSearchRoleIdsByUserId(emptyList())
        whenCalled(authGroupUserDao.searchGroupIdsByUserId(ArgumentMatchers.anyString(), anyLdt())).thenReturn(emptySet())
        // Empty effective set ⇒ findViolation queried with emptySet ⇒ null (allowed).
        whenCalled(exclusionService.findViolation("t1", "role1", emptySet())).thenReturn(null)
        assertNull(service.findSodViolationMessage("t1", "role1", "u1"))
    }

    @Test
    fun findSodViolationMessage_expandsEffectiveRolesAndFormatsMessage() {
        // direct={d1}, group g1 → {gr1}; ancestors of {d1,gr1} → {anc1}; expect exact union passed.
        stubSearchRoleIdsByUserId(listOf("d1"))
        whenCalled(authGroupUserDao.searchGroupIdsByUserId(ArgumentMatchers.anyString(), anyLdt())).thenReturn(setOf("g1"))
        whenCalled(authGroupRoleDao.searchRoleIdsByGroupId("g1")).thenReturn(setOf("gr1"))
        whenCalled(authRoleDao.searchAncestorRoleIds(listOf("d1", "gr1"))).thenReturn(setOf("anc1"))
        whenCalled(exclusionService.findViolation("t1", "role1", setOf("d1", "gr1", "anc1")))
            .thenReturn(excl("gr1", "role1", "ex7"))

        val msg = service.findSodViolationMessage("t1", "role1", "u1")
        assertNotNull(msg)
        assertTrue(msg!!.contains("User u1"))
        assertTrue(msg.contains("role1"))
        assertTrue(msg.contains("ex7"))
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
}
