package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthGroupUserService
 *
 * Test data source: `AuthGroupUserServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupUserServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authGroupUserService: IAuthGroupUserService

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    /**
     * The whole reason the sweep exists: the roles a membership confers are **cached**, and the
     * clock passing `end_time` changes no row, so nothing fires an eviction. Without the sweep, a
     * lapsed membership keeps granting the group's roles out of a snapshot that will never be
     * recomputed — the read-path filter alone does not close it.
     */
    @Test
    fun announceWindowChanges_evictsSnapshotsHoldingALapsedMembership() {
        val group = "9c1b2a3d-0000-0000-0000-000000000088"
        val role = "9c1b2a3d-0000-0000-0000-000000000087"
        val user = "9c1b2a3d-0000-0000-0000-000000000082"
        val now = LocalDateTime.now()

        val membershipId = authGroupUserService.bindTemporal(group, user, now.minusDays(1), now.plusDays(1))
        try {
            // Prime the snapshot while the membership is genuinely in force.
            assertTrue(
                roleIdsByUserIdCache.getRoleIds(user).contains(role),
                "an in-force membership must confer the group's roles",
            )

            // Close the window behind the cache's back — a DAO write publishes no domain event,
            // which is exactly what the passage of time looks like to this module.
            authGroupUserDao.update(authGroupUserDao.get(membershipId)!!.apply {
                this.endTime = now.minusSeconds(30)
            })
            assertTrue(
                authGroupUserDao.searchGroupIdsByUserId(user).isEmpty(),
                "resolution drops the lapsed membership immediately...",
            )
            assertTrue(
                roleIdsByUserIdCache.getRoleIds(user).contains(role),
                "...but the cached snapshot still confers the role, which is the gap the sweep closes",
            )

            val announced = authGroupUserService.announceWindowChanges(now.minusMinutes(1), now)
            assertEquals(1, announced, "the membership that just lapsed must be announced exactly once")

            // AFTER_COMMIT listeners do not fire inside these rolled-back tests, so the eviction the
            // announcement triggers is driven directly — same convention as RoleIdsByUserIdCacheTest.
            roleIdsByUserIdCache.on(AuthGroupUserRelationsChanged(group, listOf(user)))
            assertFalse(
                roleIdsByUserIdCache.getRoleIds(user).contains(role),
                "after the sweep the lapsed membership must confer nothing",
            )

            // A degenerate interval is a no-op rather than a re-announcement of everything.
            assertEquals(0, authGroupUserService.announceWindowChanges(now, now))
            assertEquals(0, authGroupUserService.announceWindowChanges(now, now.minusHours(1)))
        } finally {
            authGroupUserService.unbind(group, user)
        }
    }

    @Test
    fun getUserIdsByGroupId() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userIds = authGroupUserService.getUserIdsByGroupId(groupId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("9c1b2a3d-0000-0000-0000-000000000080"))
        assertTrue(userIds.contains("9c1b2a3d-0000-0000-0000-000000000081"))
    }

    @Test
    fun getGroupIdsByUserId() {
        val userId = "9c1b2a3d-0000-0000-0000-000000000080"
        val groupIds = authGroupUserService.getGroupIdsByUserId(userId)
        assertTrue(groupIds.isNotEmpty())
        assertTrue(groupIds.contains("9c1b2a3d-0000-0000-0000-000000000083"))
    }

    @Test
    fun exists() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000080"

        // Test existing relation
        assertTrue(authGroupUserService.exists(groupId, userId))

        // Test non-existing relation
        assertFalse(authGroupUserService.exists(groupId, "non-existent-user-id"))
    }

    @Test
    fun batchBind() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000084"
        val userIds = listOf(
            "9c1b2a3d-0000-0000-0000-000000000080",
            "9c1b2a3d-0000-0000-0000-000000000081",
            "9c1b2a3d-0000-0000-0000-000000000082"
        )

        // Batch bind
        val count = authGroupUserService.batchBind(groupId, userIds)
        assertTrue(count >= 3)

        // Verify binding succeeded
        val boundUserIds = authGroupUserService.getUserIdsByGroupId(groupId)
        assertTrue(boundUserIds.containsAll(userIds))

        // Test duplicate binding (should skip existing ones)
        val count2 = authGroupUserService.batchBind(groupId, userIds)
        assertTrue(count2 == 0) // Should return 0 because all already exist
    }

    @Test
    fun unbind() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000081"

        assertTrue(authGroupUserService.getUserIdsByGroupId(groupId).contains(userId))

        assertTrue(authGroupUserService.unbind(groupId, userId))

        // Resolution stops counting them immediately...
        assertFalse(authGroupUserService.getUserIdsByGroupId(groupId).contains(userId))
        // ...while the row survives, which is what keeps "who was removed, and why" answerable.
        assertTrue(authGroupUserService.exists(groupId, userId), "the membership row is kept for audit")

        authGroupUserService.batchBind(groupId, listOf(userId))
    }

    /**
     * The bug soft revocation introduces if nothing handles it: the (group, user) pair is unique, so
     * a revoked row still occupies it. Code that treats "a row exists" as "already a member" makes
     * re-adding somebody a silent no-op — the worst possible answer to "why can't they get back in".
     */
    @Test
    fun aRemovedMemberCanBeAddedBackAgain() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000080"

        assertTrue(authGroupUserService.unbind(groupId, userId, "left the team"))
        assertFalse(authGroupUserService.getUserIdsByGroupId(groupId).contains(userId))

        val reinstated = authGroupUserService.batchBind(groupId, listOf(userId))
        assertEquals(1, reinstated, "reinstating must count as a real change, not be skipped")
        assertTrue(
            authGroupUserService.getUserIdsByGroupId(groupId).contains(userId),
            "the member must actually be back in the group",
        )
    }

    /** Revoking twice is not a second revocation; the second call reports that there was nothing to do. */
    @Test
    fun unbindingAnAlreadyRemovedMemberIsANoOp() {
        val groupId = "9c1b2a3d-0000-0000-0000-000000000083"
        val userId = "9c1b2a3d-0000-0000-0000-000000000081"

        assertTrue(authGroupUserService.unbind(groupId, userId))
        assertFalse(authGroupUserService.unbind(groupId, userId))

        authGroupUserService.batchBind(groupId, listOf(userId))
    }

    /**
     * The revoked membership must stop conferring the group's roles at once — the read-path filter
     * added earlier now has a writer, and this is what proves the two halves meet.
     */
    @Test
    fun aRevokedMembershipStopsConferringTheGroupsRoles() {
        val group = "9c1b2a3d-0000-0000-0000-000000000088"
        val role = "9c1b2a3d-0000-0000-0000-000000000087"
        val user = "9c1b2a3d-0000-0000-0000-000000000082"

        authGroupUserService.batchBind(group, listOf(user))
        roleIdsByUserIdCache.on(AuthGroupUserRelationsChanged(group, listOf(user)))
        assertTrue(roleIdsByUserIdCache.getRoleIds(user).contains(role))

        authGroupUserService.unbind(group, user, "no longer needed")
        roleIdsByUserIdCache.on(AuthGroupUserRelationsChanged(group, listOf(user)))
        assertFalse(roleIdsByUserIdCache.getRoleIds(user).contains(role))
    }
}
