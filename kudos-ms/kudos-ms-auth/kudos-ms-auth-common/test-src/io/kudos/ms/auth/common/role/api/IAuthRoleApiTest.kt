package io.kudos.ms.auth.common.role.api

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for IAuthRoleApi
 *
 * Covers every interface method through a fake implementation that records dispatched arguments,
 * including the boolean returns of has* methods and empty-collection returns for unknown ids.
 *
 * @author K
 * @since 1.0.0
 */
internal class IAuthRoleApiTest {

    private fun role(id: String) = AuthRoleCacheEntry(
        id = id, code = id, name = id, tenantId = "t-1", subsysCode = "sys",
        remark = null, active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    private fun resource(id: String) = SysResourceCacheEntry(
        id = id, name = id, url = "/$id", resourceTypeDictCode = "menu", parentId = null,
        orderNum = 1, icon = null, subSystemCode = "sys", remark = null, active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    private fun user(id: String) = UserAccountCacheEntry(
        id = id, username = id, tenantId = "t-1", loginPassword = null, securityPassword = null,
        accountTypeDictCode = null, accountStatusDictCode = null, defaultLocale = null,
        defaultTimezone = null, defaultCurrency = null, lastLoginTime = null, lastLoginIp = null,
        lastLogoutTime = null, loginErrorTimes = null, securityPasswordErrorTimes = null,
        sessionKey = null, authenticationKey = null, orgId = null, supervisorId = null, remark = null,
        active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    private inner class FakeApi : IAuthRoleApi {
        override fun getRoleById(id: String): AuthRoleCacheEntry? =
            if (id == "r-1") role("r-1") else null

        override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> =
            ids.filter { it == "r-1" }.associateWith { role(it) }

        override fun getRoleId(tenantId: String, code: String): String? =
            if (tenantId == "t-1" && code == "ADMIN") "r-1" else null

        override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> =
            if (roleId == "r-1") listOf(user("u-1")) else emptyList()

        override fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String> =
            if (roleCode == "ADMIN") listOf("u-1", "u-2") else emptyList()

        override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> =
            if (roleId == "r-1") listOf(resource("res-1")) else emptyList()

        override fun hasResource(roleId: String, resourceId: String): Boolean =
            roleId == "r-1" && resourceId == "res-1"

        override fun getRoleIds(tenantId: String): List<String> =
            if (tenantId == "t-1") listOf("r-1", "r-2") else emptyList()

        override fun getResources(userId: String): List<SysResourceCacheEntry> =
            if (userId == "u-1") listOf(resource("res-1")) else emptyList()

        override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> =
            if (userId == "u-1") listOf(role("r-1")) else emptyList()

        override fun hasRole(userId: String, roleId: String): Boolean =
            userId == "u-1" && roleId == "r-1"

        override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean =
            userId == "u-1" && tenantId == "t-1" && roleCode == "ADMIN"

        override fun isUserHasResource(userId: String, resourceId: String): Boolean =
            userId == "u-1" && resourceId == "res-1"
    }

    @Test
    fun getRoleByIdAndByIds() {
        val api = FakeApi()
        assertEquals("r-1", api.getRoleById("r-1")?.id)
        assertNull(api.getRoleById("missing"))
        assertEquals(setOf("r-1"), api.getRolesByIds(listOf("r-1", "missing")).keys)
        assertTrue(api.getRolesByIds(listOf("missing")).isEmpty())
    }

    @Test
    fun getRoleIdAndIds() {
        val api = FakeApi()
        assertEquals("r-1", api.getRoleId("t-1", "ADMIN"))
        assertNull(api.getRoleId("t-1", "OTHER"))
        assertEquals(listOf("r-1", "r-2"), api.getRoleIds("t-1"))
        assertTrue(api.getRoleIds("other").isEmpty())
    }

    @Test
    fun usersAndResources() {
        val api = FakeApi()
        assertEquals("u-1", api.getRoleUsers("r-1").single().id)
        assertTrue(api.getRoleUsers("missing").isEmpty())
        assertEquals(listOf("u-1", "u-2"), api.getUserIdsByRoleCode("t-1", "ADMIN"))
        assertTrue(api.getUserIdsByRoleCode("t-1", "OTHER").isEmpty())
        assertEquals("res-1", api.getRoleResources("r-1").single().id)
        assertTrue(api.getRoleResources("missing").isEmpty())
        assertEquals("res-1", api.getResources("u-1").single().id)
        assertTrue(api.getResources("missing").isEmpty())
        assertEquals("r-1", api.getUserRoles("u-1").single().id)
        assertTrue(api.getUserRoles("missing").isEmpty())
    }

    @Test
    fun booleanChecks() {
        val api = FakeApi()
        assertTrue(api.hasResource("r-1", "res-1"))
        assertFalse(api.hasResource("r-1", "res-2"))
        assertTrue(api.hasRole("u-1", "r-1"))
        assertFalse(api.hasRole("u-2", "r-1"))
        assertTrue(api.hasRoleByCode("u-1", "t-1", "ADMIN"))
        assertFalse(api.hasRoleByCode("u-1", "t-1", "OTHER"))
        assertTrue(api.isUserHasResource("u-1", "res-1"))
        assertFalse(api.isUserHasResource("u-2", "res-1"))
    }
}
