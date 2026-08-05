package io.kudos.ms.auth.api.internal.controller.role

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.api.AuthRoleApi
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
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
 * Test for [AuthRoleInternalController].
 *
 * The controller is a pure HTTP entry-point that forwards every call verbatim to the injected
 * [AuthRoleApi]. There is no branching, transformation or DB access in it, so the core api is mocked
 * with Mockito and no Spring container / database is started.
 *
 * For each of the 13 methods the test asserts:
 * - the exact value produced by the api is returned unchanged (identity via [assertSame] where applicable);
 * - the api is invoked exactly once with the same arguments the controller received (argument pass-through);
 * - representative edge results (null miss, empty collection, false boolean, Unicode/empty-string args).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleInternalControllerTest {

    private val api = mock(AuthRoleApi::class.java)
    private val controller = AuthRoleInternalController(api)

    @Test
    fun getRoleById_returnsEntry_andDelegates() {
        val entry = mock(AuthRoleCacheEntry::class.java)
        whenCalled(api.getRoleById("role-1")).thenReturn(entry)

        assertSame(entry, controller.getRoleById("role-1"))
        verify(api).getRoleById("role-1")
    }

    @Test
    fun getRoleById_returnsNullOnMiss_andDelegates() {
        whenCalled(api.getRoleById("missing")).thenReturn(null)

        assertNull(controller.getRoleById("missing"))
        verify(api).getRoleById("missing")
    }

    @Test
    fun getRolesByIds_returnsMap_andDelegates() {
        val ids = listOf("a", "b")
        val entryA = mock(AuthRoleCacheEntry::class.java)
        val entryB = mock(AuthRoleCacheEntry::class.java)
        val expected = mapOf("a" to entryA, "b" to entryB)
        whenCalled(api.getRolesByIds(ids)).thenReturn(expected)

        val actual = controller.getRolesByIds(ids)
        assertSame(expected, actual)
        assertEquals(2, actual.size)
        assertSame(entryA, actual["a"])
        assertSame(entryB, actual["b"])
        verify(api).getRolesByIds(ids)
    }

    @Test
    fun getRolesByIds_emptyInputAndEmptyResult_delegates() {
        val ids = emptyList<String>()
        val expected = emptyMap<String, AuthRoleCacheEntry>()
        whenCalled(api.getRolesByIds(ids)).thenReturn(expected)

        val actual = controller.getRolesByIds(ids)
        assertSame(expected, actual)
        assertTrue(actual.isEmpty())
        verify(api).getRolesByIds(ids)
    }

    @Test
    fun getRolesByIds_acceptsSetCollection_delegates() {
        val ids: Collection<String> = setOf("x", "y")
        val expected = mapOf("x" to mock(AuthRoleCacheEntry::class.java))
        whenCalled(api.getRolesByIds(ids)).thenReturn(expected)

        assertSame(expected, controller.getRolesByIds(ids))
        verify(api).getRolesByIds(ids)
    }

    @Test
    fun getRoleId_returnsId_andDelegates() {
        whenCalled(api.getRoleId("tenant-1", "ADMIN")).thenReturn("role-99")

        assertEquals("role-99", controller.getRoleId("tenant-1", "ADMIN"))
        verify(api).getRoleId("tenant-1", "ADMIN")
    }

    @Test
    fun getRoleId_returnsNullOnMiss_andPreservesArgumentOrder() {
        whenCalled(api.getRoleId("tenant-1", "ADMIN")).thenReturn("role-99")
        // distinct args verify tenantId/code are not swapped by the controller
        whenCalled(api.getRoleId("ADMIN", "tenant-1")).thenReturn(null)

        assertNull(controller.getRoleId("ADMIN", "tenant-1"))
        verify(api).getRoleId("ADMIN", "tenant-1")
    }

    @Test
    fun getRoleId_unicodeArguments_delegates() {
        whenCalled(api.getRoleId("租户-甲", "管理员")).thenReturn("角色-1")

        assertEquals("角色-1", controller.getRoleId("租户-甲", "管理员"))
        verify(api).getRoleId("租户-甲", "管理员")
    }

    @Test
    fun getRoleUsers_returnsList_andDelegates() {
        val user = mock(UserAccountCacheEntry::class.java)
        val expected = listOf(user)
        whenCalled(api.getRoleUsers("role-1")).thenReturn(expected)

        val actual = controller.getRoleUsers("role-1")
        assertSame(expected, actual)
        assertSame(user, actual[0])
        verify(api).getRoleUsers("role-1")
    }

    @Test
    fun getRoleUsers_emptyList_delegates() {
        val expected = emptyList<UserAccountCacheEntry>()
        whenCalled(api.getRoleUsers("role-empty")).thenReturn(expected)

        assertSame(expected, controller.getRoleUsers("role-empty"))
        verify(api).getRoleUsers("role-empty")
    }

    @Test
    fun getUserIdsByRoleCode_returnsList_andPreservesOrder() {
        val expected = listOf("u3", "u1", "u2")
        whenCalled(api.getUserIdsByRoleCode("tenant-1", "ADMIN")).thenReturn(expected)

        val actual = controller.getUserIdsByRoleCode("tenant-1", "ADMIN")
        assertSame(expected, actual)
        assertEquals(listOf("u3", "u1", "u2"), actual)
        verify(api).getUserIdsByRoleCode("tenant-1", "ADMIN")
    }

    @Test
    fun getUserIdsByRoleCode_emptyList_andArgumentOrder() {
        whenCalled(api.getUserIdsByRoleCode("ADMIN", "tenant-1")).thenReturn(emptyList())

        assertTrue(controller.getUserIdsByRoleCode("ADMIN", "tenant-1").isEmpty())
        verify(api).getUserIdsByRoleCode("ADMIN", "tenant-1")
    }

    @Test
    fun getRoleResources_returnsList_andDelegates() {
        val res = mock(SysResourceCacheEntry::class.java)
        val expected = listOf(res)
        whenCalled(api.getRoleResources("role-1")).thenReturn(expected)

        val actual = controller.getRoleResources("role-1")
        assertSame(expected, actual)
        assertSame(res, actual[0])
        verify(api).getRoleResources("role-1")
    }

    @Test
    fun getRoleResources_emptyList_delegates() {
        whenCalled(api.getRoleResources("role-empty")).thenReturn(emptyList())

        assertTrue(controller.getRoleResources("role-empty").isEmpty())
        verify(api).getRoleResources("role-empty")
    }

    @Test
    fun hasResource_true_andFalse_withArgumentOrder() {
        whenCalled(api.hasResource("role-1", "res-1")).thenReturn(true)
        whenCalled(api.hasResource("res-1", "role-1")).thenReturn(false)

        assertTrue(controller.hasResource("role-1", "res-1"))
        assertFalse(controller.hasResource("res-1", "role-1"))
        verify(api).hasResource("role-1", "res-1")
        verify(api).hasResource("res-1", "role-1")
    }

    @Test
    fun getRoleIds_returnsList_andDelegates() {
        val expected = listOf("r1", "r2")
        whenCalled(api.getRoleIds("tenant-1")).thenReturn(expected)

        assertSame(expected, controller.getRoleIds("tenant-1"))
        verify(api).getRoleIds("tenant-1")
    }

    @Test
    fun getRoleIds_emptyList_delegates() {
        whenCalled(api.getRoleIds("tenant-empty")).thenReturn(emptyList())

        assertTrue(controller.getRoleIds("tenant-empty").isEmpty())
        verify(api).getRoleIds("tenant-empty")
    }

    @Test
    fun getResources_returnsList_andDelegates() {
        val res = mock(SysResourceCacheEntry::class.java)
        val expected = listOf(res)
        whenCalled(api.getResources("user-1")).thenReturn(expected)

        val actual = controller.getResources("user-1")
        assertSame(expected, actual)
        assertSame(res, actual[0])
        verify(api).getResources("user-1")
    }

    @Test
    fun getResources_emptyList_delegates() {
        whenCalled(api.getResources("user-empty")).thenReturn(emptyList())

        assertTrue(controller.getResources("user-empty").isEmpty())
        verify(api).getResources("user-empty")
    }

    @Test
    fun getUserRoles_returnsList_andDelegates() {
        val role = mock(AuthRoleCacheEntry::class.java)
        val expected = listOf(role)
        whenCalled(api.getUserRoles("user-1")).thenReturn(expected)

        val actual = controller.getUserRoles("user-1")
        assertSame(expected, actual)
        assertSame(role, actual[0])
        verify(api).getUserRoles("user-1")
    }

    @Test
    fun getUserRoles_emptyList_delegates() {
        whenCalled(api.getUserRoles("user-empty")).thenReturn(emptyList())

        assertTrue(controller.getUserRoles("user-empty").isEmpty())
        verify(api).getUserRoles("user-empty")
    }

    @Test
    fun hasRole_true_andFalse_withArgumentOrder() {
        whenCalled(api.hasRole("user-1", "role-1")).thenReturn(true)
        whenCalled(api.hasRole("role-1", "user-1")).thenReturn(false)

        assertTrue(controller.hasRole("user-1", "role-1"))
        assertFalse(controller.hasRole("role-1", "user-1"))
        verify(api).hasRole("user-1", "role-1")
        verify(api).hasRole("role-1", "user-1")
    }

    @Test
    fun hasRoleByCode_true_andFalse_withThreeArgumentOrder() {
        whenCalled(api.hasRoleByCode("user-1", "tenant-1", "ADMIN")).thenReturn(true)
        // permuted args must reach the api in the same permuted order (not normalized)
        whenCalled(api.hasRoleByCode("tenant-1", "ADMIN", "user-1")).thenReturn(false)

        assertTrue(controller.hasRoleByCode("user-1", "tenant-1", "ADMIN"))
        assertFalse(controller.hasRoleByCode("tenant-1", "ADMIN", "user-1"))
        verify(api).hasRoleByCode("user-1", "tenant-1", "ADMIN")
        verify(api).hasRoleByCode("tenant-1", "ADMIN", "user-1")
    }

    @Test
    fun isUserHasResource_true_andFalse_withArgumentOrder() {
        whenCalled(api.isUserHasResource("user-1", "res-1")).thenReturn(true)
        whenCalled(api.isUserHasResource("res-1", "user-1")).thenReturn(false)

        assertTrue(controller.isUserHasResource("user-1", "res-1"))
        assertFalse(controller.isUserHasResource("res-1", "user-1"))
        verify(api).isUserHasResource("user-1", "res-1")
        verify(api).isUserHasResource("res-1", "user-1")
    }

    @Test
    fun emptyStringArguments_arePassedThroughVerbatim() {
        whenCalled(api.getRoleById("")).thenReturn(null)
        whenCalled(api.hasRole("", "")).thenReturn(false)

        assertNull(controller.getRoleById(""))
        assertFalse(controller.hasRole("", ""))
        verify(api).getRoleById("")
        verify(api).hasRole("", "")
    }
}
