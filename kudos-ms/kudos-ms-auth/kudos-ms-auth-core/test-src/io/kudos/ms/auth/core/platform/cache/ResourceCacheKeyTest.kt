package io.kudos.ms.auth.core.platform.cache

import io.kudos.ms.auth.core.group.cache.UserIdsByTenantIdAndGroupCodeCache
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure-logic test for the composite cache-key builders ([getKey]) of the (tenant, *)-indexed caches.
 *
 * getKey joins its parts with the framework delimiter "::" and touches none of the injected
 * collaborators, so a bare instance is enough — no Spring / Redis / DB. Covers the by-group-code,
 * by-role-code and by-username variants in the platform package plus the group-package
 * UserIdsByTenantIdAndGroupCodeCache, pinning the exact key format (including blank / Unicode parts).
 *
 * @author K
 * @since 1.0.0
 */
internal class ResourceCacheKeyTest {

    private val byGroupCode = ResourceIdsByTenantIdAndGroupCodeCache()
    private val byRoleCode = ResourceIdsByTenantIdAndRoleCodeCache()
    private val byUsername = ResourceIdsByTenantIdAndUsernameCache()
    private val userIdsByGroupCode = UserIdsByTenantIdAndGroupCodeCache()

    @Test
    fun getKey_joinsTenantAndGroupCodeWithDelimiter() {
        assertEquals("t1::GROUP_ADMIN", byGroupCode.getKey("t1", "GROUP_ADMIN"))
        assertEquals("t1::GROUP_ADMIN", userIdsByGroupCode.getKey("t1", "GROUP_ADMIN"))
    }

    @Test
    fun getKey_joinsTenantAndRoleCodeWithDelimiter() {
        assertEquals("t1::ROLE_USER", byRoleCode.getKey("t1", "ROLE_USER"))
    }

    @Test
    fun getKey_joinsTenantAndUsernameWithDelimiter() {
        assertEquals("t1::alice", byUsername.getKey("t1", "alice"))
    }

    @Test
    fun getKey_preservesBlankAndUnicodeParts() {
        // Blank parts still produce a delimiter-joined key (no trimming / collapsing).
        assertEquals("::", byGroupCode.getKey("", ""))
        assertEquals("租户::用户组", byGroupCode.getKey("租户", "用户组"))
    }
}
