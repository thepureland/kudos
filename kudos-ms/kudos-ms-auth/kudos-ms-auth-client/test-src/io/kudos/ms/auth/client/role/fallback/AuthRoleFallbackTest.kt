package io.kudos.ms.auth.client.role.fallback

import io.kudos.ms.auth.client.role.proxy.IAuthRoleProxy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [AuthRoleFallback].
 *
 * The fallback is the fail-closed degradation path for the role Feign proxy: every read returns a
 * safe default (null / empty collection / empty map) and every authorization check returns `false`
 * (deny). These tests assert that contract across all 14 methods, including edge inputs
 * (empty strings, empty/null collections, Unicode) so a regression that flips any check to
 * fail-open (return `true`) or leaks a non-empty default is caught.
 *
 * No Spring context or DB is started: the fallback is a plain bean instantiated directly.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleFallbackTest {

    private val fallback = AuthRoleFallback()

    @Test
    fun isAnIAuthRoleProxy() {
        // The fallback must satisfy the proxy interface so Feign can substitute it.
        val proxy: Any = fallback
        assertTrue(proxy is IAuthRoleProxy)
    }

    @Test
    fun getRoleById_returnsNull() {
        assertNull(fallback.getRoleById("role-1"))
        assertNull(fallback.getRoleById(""))
        assertNull(fallback.getRoleById("角色-ünï"))
    }

    @Test
    fun getRolesByIds_returnsEmptyMap() {
        assertTrue(fallback.getRolesByIds(listOf("a", "b")).isEmpty())
        assertTrue(fallback.getRolesByIds(emptyList()).isEmpty())
        assertTrue(fallback.getRolesByIds(setOf("角色")).isEmpty())
    }

    @Test
    fun getRoleId_returnsNull() {
        assertNull(fallback.getRoleId("tenant-1", "ADMIN"))
        assertNull(fallback.getRoleId("", ""))
        assertNull(fallback.getRoleId("租戶", "管理員-ünï"))
    }

    @Test
    fun getRoleUsers_returnsEmptyList() {
        assertTrue(fallback.getRoleUsers("role-1").isEmpty())
        assertTrue(fallback.getRoleUsers("").isEmpty())
        assertTrue(fallback.getRoleUsers("角色-ünï").isEmpty())
    }

    @Test
    fun getUserIdsByRoleCode_returnsEmptyList() {
        assertTrue(fallback.getUserIdsByRoleCode("tenant-1", "ADMIN").isEmpty())
        assertTrue(fallback.getUserIdsByRoleCode("", "").isEmpty())
        assertTrue(fallback.getUserIdsByRoleCode("租戶", "角色碼").isEmpty())
    }

    @Test
    fun getRoleResources_returnsEmptyList() {
        assertTrue(fallback.getRoleResources("role-1").isEmpty())
        assertTrue(fallback.getRoleResources("").isEmpty())
        assertTrue(fallback.getRoleResources("角色-ünï").isEmpty())
    }

    @Test
    fun hasResource_returnsFalse_failClosed() {
        assertFalse(fallback.hasResource("role-1", "res-1"))
        assertFalse(fallback.hasResource("", ""))
        assertFalse(fallback.hasResource("角色", "資源-ünï"))
    }

    @Test
    fun getRoleIds_returnsEmptyList() {
        assertTrue(fallback.getRoleIds("tenant-1").isEmpty())
        assertTrue(fallback.getRoleIds("").isEmpty())
        assertTrue(fallback.getRoleIds("租戶-ünï").isEmpty())
    }

    @Test
    fun getResources_byUserId_returnsEmptyList() {
        assertTrue(fallback.getResources("user-1").isEmpty())
        assertTrue(fallback.getResources("").isEmpty())
        assertTrue(fallback.getResources("用戶-ünï").isEmpty())
    }

    @Test
    fun getUserRoles_returnsEmptyList() {
        assertTrue(fallback.getUserRoles("user-1").isEmpty())
        assertTrue(fallback.getUserRoles("").isEmpty())
        assertTrue(fallback.getUserRoles("用戶-ünï").isEmpty())
    }

    @Test
    fun hasRole_returnsFalse_failClosed() {
        assertFalse(fallback.hasRole("user-1", "role-1"))
        assertFalse(fallback.hasRole("", ""))
        assertFalse(fallback.hasRole("用戶", "角色-ünï"))
    }

    @Test
    fun hasRoleByCode_returnsFalse_failClosed() {
        assertFalse(fallback.hasRoleByCode("user-1", "tenant-1", "ADMIN"))
        assertFalse(fallback.hasRoleByCode("", "", ""))
        assertFalse(fallback.hasRoleByCode("用戶", "租戶", "角色碼-ünï"))
    }

    @Test
    fun isUserHasResource_returnsFalse_failClosed() {
        assertFalse(fallback.isUserHasResource("user-1", "res-1"))
        assertFalse(fallback.isUserHasResource("", ""))
        assertFalse(fallback.isUserHasResource("用戶", "資源-ünï"))
    }

    @Test
    fun emptyMap_isSameInstance_acrossCalls() {
        // emptyMap()/emptyList() return shared singletons; assert no per-call allocation surprises
        // and that defaults are genuinely empty immutable singletons.
        assertSame(fallback.getRolesByIds(emptyList()), fallback.getRolesByIds(listOf("x")))
        assertSame(fallback.getRoleUsers("a"), fallback.getRoleUsers("b"))
        assertSame(fallback.getRoleIds("a"), fallback.getRoleIds("b"))
    }
}
