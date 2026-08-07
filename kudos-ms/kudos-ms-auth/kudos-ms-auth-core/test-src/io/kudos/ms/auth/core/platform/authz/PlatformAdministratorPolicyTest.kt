package io.kudos.ms.auth.core.platform.authz

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Security contract for the non-forgeable platform-administrator identity.
 *
 * @author K
 * @author AI: Codex
 */
internal class PlatformAdministratorPolicyTest {

    private val properties = AuthzProperties().apply {
        platformAdminRoleCodes = setOf("PLATFORM_ADMIN")
        platformAdminTenantIds = setOf("platform")
    }
    private val roleIds = mock(RoleIdsByUserIdCache::class.java)
    private val roles = mock(AuthRoleHashCache::class.java)
    private val policy = PlatformAdministratorPolicy().apply {
        inject("properties", properties)
        inject("roleIdsByUserIdCache", roleIds)
        inject("authRoleHashCache", roles)
    }

    @Test
    fun onlyBuiltInNonDelegableRoleInConfiguredTenantQualifies() {
        assertTrue(evaluate(role("platform", builtIn = true, delegableMax = 0)))
    }

    @Test
    fun sameCodeInOrdinaryTenantCannotForgePlatformAdministrator() {
        assertFalse(evaluate(role("customer-a", builtIn = true, delegableMax = 0)))
    }

    @Test
    fun mutableOrDelegableRoleCannotBecomePlatformAdministrator() {
        assertFalse(evaluate(role("platform", builtIn = false, delegableMax = 0)))
        assertFalse(evaluate(role("platform", builtIn = true, delegableMax = 1)))
    }

    @Test
    fun missingDelegationCeilingCannotBecomePlatformAdministrator() {
        assertFalse(evaluate(role("platform", builtIn = true, delegableMax = null)))
    }

    private fun evaluate(role: AuthRoleCacheEntry): Boolean {
        whenCalled(roleIds.getRoleIds("u1")).thenReturn(listOf(role.id))
        whenCalled(roles.getRolesByIds(listOf(role.id))).thenReturn(mapOf(role.id to role))
        return policy.isPlatformAdministrator("u1")
    }

    private fun role(tenantId: String, builtIn: Boolean, delegableMax: Int?) = AuthRoleCacheEntry(
        id = "r1",
        code = "PLATFORM_ADMIN",
        name = "Platform administrator",
        tenantId = tenantId,
        subsysCode = "auth",
        remark = null,
        active = true,
        builtIn = builtIn,
        createUserId = null,
        createUserName = null,
        createTime = null,
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
        delegableMax = delegableMax,
    )

    private fun Any.inject(field: String, value: Any) {
        val f = this::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }
}
