package io.kudos.ms.auth.core.role.api

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.UserIdsByTenantIdAndRoleCodeCache
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure-delegation unit test for [AuthRoleApi] (collaborators mocked with Mockito, no Spring container).
 *
 * AuthRoleApi forwards each call to one of three private `@Resource lateinit var` collaborators
 * (AuthRoleHashCache / IAuthRoleService / UserIdsByTenantIdAndRoleCodeCache), injected here by
 * reflection. Each test asserts the method forwards arguments unchanged and returns the
 * collaborator's result verbatim — including the `getRoleId` mapping that returns the entry's id
 * when present and null when the role is absent.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleApiTest {

    private val hashCache = mock(AuthRoleHashCache::class.java)
    private val service = mock(IAuthRoleService::class.java)
    private val tenantRoleCache = mock(UserIdsByTenantIdAndRoleCodeCache::class.java)

    private val api = AuthRoleApi().apply {
        inject("authRoleHashCache", hashCache)
        inject("authRoleService", service)
        inject("userIdsByTenantIdAndRoleCodeCache", tenantRoleCache)
    }

    private fun AuthRoleApi.inject(fieldName: String, value: Any) {
        val f = AuthRoleApi::class.java.getDeclaredField(fieldName)
        f.isAccessible = true
        f.set(this, value)
    }

    private fun entry(id: String) = AuthRoleCacheEntry(
        id = id, code = "C_$id", name = "N_$id", tenantId = "t1", subsysCode = "ams",
        remark = null, active = true, builtIn = false, createUserId = null, createUserName = null,
        createTime = null, updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Test
    fun getRoleById_delegatesToHashCache() {
        val e = entry("r1")
        whenCalled(hashCache.getRoleById("r1")).thenReturn(e)
        whenCalled(hashCache.getRoleById("missing")).thenReturn(null)
        assertSame(e, api.getRoleById("r1"))
        assertNull(api.getRoleById("missing"))
        verify(hashCache).getRoleById("r1")
    }

    @Test
    fun getRolesByIds_delegatesToHashCache() {
        val map = mapOf("r1" to entry("r1"), "r2" to entry("r2"))
        whenCalled(hashCache.getRolesByIds(listOf("r1", "r2"))).thenReturn(map)
        assertEquals(map, api.getRolesByIds(listOf("r1", "r2")))
        verify(hashCache).getRolesByIds(listOf("r1", "r2"))
    }

    @Test
    fun getRoleId_returnsIdWhenPresent() {
        whenCalled(hashCache.getRoleByTenantIdAndRoleCode("t1", "ROLE_X")).thenReturn(entry("r1"))
        assertEquals("r1", api.getRoleId("t1", "ROLE_X"))
    }

    @Test
    fun getRoleId_returnsNullWhenAbsent() {
        whenCalled(hashCache.getRoleByTenantIdAndRoleCode("t1", "ROLE_MISSING")).thenReturn(null)
        assertNull(api.getRoleId("t1", "ROLE_MISSING"))
    }

    @Test
    fun getUserIdsByRoleCode_delegatesToTenantRoleCache() {
        whenCalled(tenantRoleCache.getUserIds("t1", "ROLE_X")).thenReturn(listOf("u1", "u2"))
        assertEquals(listOf("u1", "u2"), api.getUserIdsByRoleCode("t1", "ROLE_X"))
        verify(tenantRoleCache).getUserIds("t1", "ROLE_X")
    }

    @Test
    fun getRoleUsers_delegatesToService() {
        whenCalled(service.getRoleUsers("role1")).thenReturn(emptyList())
        assertTrue(api.getRoleUsers("role1").isEmpty())
        verify(service).getRoleUsers("role1")
    }

    @Test
    fun getRoleResources_delegatesToService() {
        whenCalled(service.getRoleResources("role1")).thenReturn(emptyList())
        assertTrue(api.getRoleResources("role1").isEmpty())
        verify(service).getRoleResources("role1")
    }

    @Test
    fun hasResource_delegatesToService() {
        whenCalled(service.hasResource("role1", "res1")).thenReturn(true)
        whenCalled(service.hasResource("role2", "res2")).thenReturn(false)
        assertTrue(api.hasResource("role1", "res1"))
        assertFalse(api.hasResource("role2", "res2"))
    }

    @Test
    fun getRoleIds_delegatesToService() {
        whenCalled(service.getRoleIds("t1")).thenReturn(listOf("r1", "r2"))
        assertEquals(listOf("r1", "r2"), api.getRoleIds("t1"))
        verify(service).getRoleIds("t1")
    }

    @Test
    fun getResources_delegatesToService() {
        whenCalled(service.getResources("u1")).thenReturn(emptyList())
        assertTrue(api.getResources("u1").isEmpty())
        verify(service).getResources("u1")
    }

    @Test
    fun getUserRoles_delegatesToService() {
        val roles = listOf(entry("r1"))
        whenCalled(service.getUserRoles("u1")).thenReturn(roles)
        assertEquals(roles, api.getUserRoles("u1"))
        verify(service).getUserRoles("u1")
    }

    @Test
    fun hasRole_delegatesToService() {
        whenCalled(service.hasRole("u1", "r1")).thenReturn(true)
        assertTrue(api.hasRole("u1", "r1"))
        verify(service).hasRole("u1", "r1")
    }

    @Test
    fun hasRoleByCode_delegatesToService() {
        whenCalled(service.hasRoleByCode("u1", "t1", "ROLE_X")).thenReturn(true)
        assertTrue(api.hasRoleByCode("u1", "t1", "ROLE_X"))
        verify(service).hasRoleByCode("u1", "t1", "ROLE_X")
    }

    @Test
    fun isUserHasResource_delegatesToService() {
        whenCalled(service.isUserHasResource("u1", "res1")).thenReturn(true)
        assertTrue(api.isUserHasResource("u1", "res1"))
        verify(service).isUserHasResource("u1", "res1")
    }
}
