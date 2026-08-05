package io.kudos.ms.auth.core.role.datascope.service

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.datascope.dao.AuthRoleScopeDao
import io.kudos.ms.auth.core.role.datascope.service.impl.AuthRoleDataScopeService
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import io.kudos.ms.auth.core.role.datascope.spi.IScopeDimensionProvider
import io.kudos.ms.auth.core.role.datascope.cache.DataScopeByUserIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthRoleDataScopeService] (collaborators mocked with Mockito; no Spring
 * container, no DB) — complements the container-backed [AuthRoleDataScopeServiceTest] by covering
 * the branches the integration fixture cannot reach deterministically:
 *
 *  - updateScope: code normalisation (blank/whitespace ⇒ null/ALL), unknown-code rejection, the
 *    DB-failure path (update returns false ⇒ log.error, no event published), and the success path
 *    (event published, lowercase code accepted via DataScopeEnum.fromCode's uppercasing).
 *  - bindOrgs: whitespace trimming + blank-filtering of org ids, and the all-blank ⇒ 0 short-circuit.
 *  - getOrgIdsByRoleId delegation.
 *
 * The resolveUserDataScope merge logic is exercised end-to-end by the container test; here we add
 * only the deterministic edge of an empty effective role set returning self-only.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleDataScopeServiceUnitTest {

    private val dao = mock(AuthRoleScopeDao::class.java)
    private val authRoleHashCache = mock(AuthRoleHashCache::class.java)
    private val roleIdsByUserIdCache = mock(RoleIdsByUserIdCache::class.java)
    // Org is no longer read from the user tables directly: it is one registered dimension among
    // several, reached through the SPI so an application-defined dimension works the same way.
    private val orgProvider = mock(IScopeDimensionProvider::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val eventPublisher = mock(org.springframework.context.ApplicationEventPublisher::class.java)

    private val service = AuthRoleDataScopeService(dao).apply {
        inject("authRoleHashCache", authRoleHashCache)
        inject("roleIdsByUserIdCache", roleIdsByUserIdCache)
        inject("dimensionProviders", listOf(orgProvider))
        inject("authRoleDao", authRoleDao)
        inject("eventPublisher", eventPublisher)
        inject("authRoleUserDao", mock(AuthRoleUserDao::class.java))
        inject("authGroupDao", mock(AuthGroupDao::class.java))
        inject("authGroupUserDao", mock(AuthGroupUserDao::class.java))
        inject("authGroupRoleDao", mock(AuthGroupRoleDao::class.java))
        inject("dataScopeByUserIdCache", mock(DataScopeByUserIdCache::class.java))
    }

    private fun AuthRoleDataScopeService.inject(field: String, value: Any) {
        val f = AuthRoleDataScopeService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    // Kotlin non-null params reject Mockito's null-returning matchers; coalesce to a real value.
    private fun anyRole(): AuthRole = ArgumentMatchers.any(AuthRole::class.java) ?: AuthRole { id = "x" }

    private fun anyEvent(): Any = ArgumentMatchers.any(Any::class.java) ?: Any()

    @Suppress("UNCHECKED_CAST")
    private fun anyCollection(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun anyStringCollection(): Collection<String> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<String>?) ?: emptyList()

    private fun captureRole(captor: org.mockito.ArgumentCaptor<AuthRole>): AuthRole =
        captor.capture() ?: AuthRole { id = "x" }

    // ---------------------------------------------------------------- updateScope

    @Test
    fun updateScope_validCode_succeeds_andPublishesEvent() {
        whenCalled(authRoleDao.update(anyRole())).thenReturn(true)
        assertTrue(service.updateScope("role1", "ORG"))
        verify(eventPublisher).publishEvent(AuthRoleUpdated("role1"))
    }

    @Test
    fun updateScope_lowercaseCode_acceptedViaUppercasing() {
        // DataScopeEnum.fromCode uppercases, so "org" is recognised.
        whenCalled(authRoleDao.update(anyRole())).thenReturn(true)
        assertTrue(service.updateScope("role1", "org"))
    }

    @Test
    fun updateScope_nullMeansAll_succeeds() {
        whenCalled(authRoleDao.update(anyRole())).thenReturn(true)
        assertTrue(service.updateScope("role1", null))
        // Captured PO must carry a null dataScope (ALL).
        val captor = org.mockito.ArgumentCaptor.forClass(AuthRole::class.java)
        verify(authRoleDao).update(captureRole(captor))
        assertEquals(null, captor.value.dataScope)
    }

    @Test
    fun updateScope_blankCode_normalisedToNull() {
        whenCalled(authRoleDao.update(anyRole())).thenReturn(true)
        assertTrue(service.updateScope("role1", "   "))
        val captor = org.mockito.ArgumentCaptor.forClass(AuthRole::class.java)
        verify(authRoleDao).update(captureRole(captor))
        assertEquals(null, captor.value.dataScope, "blank ⇒ null (ALL)")
    }

    @Test
    fun updateScope_unknownCode_rejected() {
        val err = assertFailsWith<IllegalArgumentException> { service.updateScope("role1", "BOGUS") }
        assertTrue(err.message!!.contains("Unknown data scope code"))
        verify(authRoleDao, never()).update(anyRole())
    }

    @Test
    fun updateScope_daoReturnsFalse_noEventPublished() {
        whenCalled(authRoleDao.update(anyRole())).thenReturn(false)
        assertFalse(service.updateScope("role1", "SELF"))
        verify(eventPublisher, never()).publishEvent(anyEvent())
    }

    // ---------------------------------------------------------------- bindOrgs

    @Test
    fun bindOrgs_trimsAndFiltersBlanks() {
        whenCalled(dao.deleteByRoleId("role1", AuthRoleScopeDao.DIMENSION_ORG)).thenReturn(0)
        whenCalled(dao.batchInsert(anyCollection(), ArgumentMatchers.anyInt())).thenReturn(2)
        val n = service.bindOrgs("role1", listOf(" o1 ", "", "   ", "o2"))
        assertEquals(2, n)
        verify(dao).deleteByRoleId("role1")
    }

    @Test
    fun bindOrgs_deduplicates() {
        whenCalled(dao.deleteByRoleId("role1", AuthRoleScopeDao.DIMENSION_ORG)).thenReturn(0)
        whenCalled(dao.batchInsert(anyCollection(), ArgumentMatchers.anyInt())).thenReturn(1)
        val n = service.bindOrgs("role1", listOf("o1", "o1", " o1 "))
        assertEquals(1, n)
    }

    @Test
    fun bindOrgs_allBlank_clearsAndReturnsZero() {
        whenCalled(dao.deleteByRoleId("role1", AuthRoleScopeDao.DIMENSION_ORG)).thenReturn(3)
        val n = service.bindOrgs("role1", listOf("", "   "))
        assertEquals(0, n)
        verify(dao).deleteByRoleId("role1")
        // No insert when nothing concrete remains.
        verify(dao, never()).batchInsert(anyCollection(), ArgumentMatchers.anyInt())
    }

    @Test
    fun bindOrgs_empty_clearsAndReturnsZero() {
        whenCalled(dao.deleteByRoleId("role1", AuthRoleScopeDao.DIMENSION_ORG)).thenReturn(0)
        assertEquals(0, service.bindOrgs("role1", emptyList()))
        verify(dao, never()).batchInsert(anyCollection(), ArgumentMatchers.anyInt())
    }

    // ---------------------------------------------------------------- getOrgIdsByRoleId

    @Test
    fun getOrgIdsByRoleId_delegatesToDao() {
        whenCalled(dao.searchValuesByRoleId("role1", AuthRoleScopeDao.DIMENSION_ORG)).thenReturn(setOf("o1", "o2"))
        assertEquals(setOf("o1", "o2"), service.getOrgIdsByRoleId("role1"))
        verify(dao).searchValuesByRoleId("role1", AuthRoleScopeDao.DIMENSION_ORG)
    }

    // ---------------------------------------------------------------- resolveUserDataScope edges

    @Test
    fun resolve_userWithoutRoles_selfOnly() {
        whenCalled(roleIdsByUserIdCache.getRoleIds("u-none")).thenReturn(emptyList())
        val vo = service.computeUserDataScope("u-none")
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun resolve_anyAllScope_shortCircuitsToAll() {
        whenCalled(roleIdsByUserIdCache.getRoleIds("u1")).thenReturn(listOf("rAll", "rSelf"))
        whenCalled(authRoleHashCache.getRolesByIds(anyStringCollection())).thenReturn(
            mapOf(
                "rAll" to entry("rAll", "ALL"),
                "rSelf" to entry("rSelf", "SELF"),
            )
        )
        val vo = service.computeUserDataScope("u1")
        assertTrue(vo.all)
        // ALL short-circuits before any org lookup.
        verify(orgProvider, never()).expand(anyStringSet())
    }

    @Test
    fun resolve_nullDataScopeTreatedAsAll() {
        whenCalled(roleIdsByUserIdCache.getRoleIds("u2")).thenReturn(listOf("rNull"))
        whenCalled(authRoleHashCache.getRolesByIds(anyStringCollection())).thenReturn(mapOf("rNull" to entry("rNull", null)))
        assertTrue(service.computeUserDataScope("u2").all)
    }

    @Test
    fun resolve_customWithNoGrants_andNoOther_fallsBackToSelfOnly() {
        whenCalled(roleIdsByUserIdCache.getRoleIds("u3")).thenReturn(listOf("rCustom"))
        whenCalled(authRoleHashCache.getRolesByIds(anyStringCollection())).thenReturn(mapOf("rCustom" to entry("rCustom", "CUSTOM")))
        whenCalled(dao.searchAllByRoleId("rCustom")).thenReturn(emptyMap())
        val vo = service.computeUserDataScope("u3")
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun resolve_selfScope_setsSelfFlag() {
        whenCalled(roleIdsByUserIdCache.getRoleIds("u4")).thenReturn(listOf("rSelf"))
        whenCalled(authRoleHashCache.getRolesByIds(anyStringCollection())).thenReturn(mapOf("rSelf" to entry("rSelf", "SELF")))
        val vo = service.computeUserDataScope("u4")
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    private fun entry(id: String, dataScope: String?) = AuthRoleCacheEntry(
        id = id, code = "C_$id", name = "N_$id", tenantId = "t1", subsysCode = "ams",
        dataScope = dataScope, remark = null, active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Suppress("UNCHECKED_CAST")
    private fun anyStringSet(): Set<String> =
        (ArgumentMatchers.any(Set::class.java) as Set<String>?) ?: emptySet()
}
