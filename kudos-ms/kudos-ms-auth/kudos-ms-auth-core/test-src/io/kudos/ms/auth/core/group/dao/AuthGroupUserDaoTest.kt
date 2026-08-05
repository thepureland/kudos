package io.kudos.ms.auth.core.group.dao

import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for AuthGroupUserDao
 *
 * Test data source: `AuthGroupUserDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun exists() {
        // Test existing relation
        assertTrue(authGroupUserDao.exists("a1b2c3d4-0000-0000-0000-000000000072", "a1b2c3d4-0000-0000-0000-000000000070"))

        // Test non-existing relation
        assertFalse(authGroupUserDao.exists("a1b2c3d4-0000-0000-0000-000000000072", "non-existent-user"))
        assertFalse(authGroupUserDao.exists("non-existent-group", "a1b2c3d4-0000-0000-0000-000000000070"))
    }

    @Test
    fun searchUserIdsByGroupId() {
        val groupId = "a1b2c3d4-0000-0000-0000-000000000072"
        val userIds = authGroupUserDao.searchUserIdsByGroupId(groupId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("a1b2c3d4-0000-0000-0000-000000000070"))
        assertTrue(userIds.contains("a1b2c3d4-0000-0000-0000-000000000071"))

        // Test non-existing group ID
        val emptyUserIds = authGroupUserDao.searchUserIdsByGroupId("non-existent-group")
        assertTrue(emptyUserIds.isEmpty())
    }

    @Test
    fun searchGroupIdsByUserId() {
        val userId = "a1b2c3d4-0000-0000-0000-000000000070"
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        assertTrue(groupIds.isNotEmpty())
        assertTrue(groupIds.contains("a1b2c3d4-0000-0000-0000-000000000072"))

        // Test non-existing user ID
        val emptyGroupIds = authGroupUserDao.searchGroupIdsByUserId("non-existent-user")
        assertTrue(emptyGroupIds.isEmpty())
    }

    /**
     * Group membership must be able to express everything a direct grant can — a validity window
     * included. Without parity the group path is simply the way to opt out of the constraints the
     * direct path enforces.
     */
    @Test
    fun membershipGovernanceColumns_roundTrip() {
        val start = LocalDateTime.now().minusDays(1).withNano(0)
        val end = LocalDateTime.now().plusDays(1).withNano(0)
        val id = authGroupUserDao.insert(AuthGroupUser.Companion().apply {
            this.groupId = "a1b2c3d4-0000-0000-0000-000000000072"
            this.userId = "membership-governance-user"
            this.principalType = "SERVICE"
            this.startTime = start
            this.endTime = end
            this.grantedBy = "some-admin"
            this.parentGrantId = "upstream-grant-id"
            this.revoked = false
        })
        try {
            val row = authGroupUserDao.get(id)!!
            assertEquals("SERVICE", row.principalType)
            assertEquals(start, row.startTime)
            assertEquals(end, row.endTime)
            assertEquals("some-admin", row.grantedBy)
            assertEquals("upstream-grant-id", row.parentGrantId)
            assertEquals(false, row.revoked)
        } finally {
            authGroupUserDao.deleteById(id)
        }
    }

    /**
     * The defect this locks down: the write path stored `end_time` and `revoked`, the admin UI
     * offered both, and every read path ignored them — so a lapsed or revoked membership went on
     * handing over every role the group carried. Group membership must obey its own window, or
     * "add them to the group instead" is the documented way around an expiry somebody set.
     */
    @Test
    fun searchGroupIdsByUserId_countsOnlyMembershipsInForce() {
        val group = "a1b2c3d4-0000-0000-0000-0000000000f0"
        val now = LocalDateTime.now()
        val live = uid("wlive")
        val lapsed = uid("wlapsed")
        val future = uid("wfuture")
        val revoked = uid("wrevoked")
        val permanent = uid("wperm")
        val ids = listOf(
            insertMembership(group, live, start = now.minusDays(1), end = now.plusDays(1)),
            insertMembership(group, lapsed, start = now.minusDays(2), end = now.minusDays(1)),
            insertMembership(group, future, start = now.plusDays(1), end = now.plusDays(2)),
            insertMembership(group, revoked, revoked = true),
            insertMembership(group, permanent),
        )
        try {
            assertEquals(setOf(group), authGroupUserDao.searchGroupIdsByUserId(live))
            assertEquals(setOf(group), authGroupUserDao.searchGroupIdsByUserId(permanent))
            assertTrue(
                authGroupUserDao.searchGroupIdsByUserId(lapsed).isEmpty(),
                "an expired membership must stop contributing the group's roles",
            )
            assertTrue(
                authGroupUserDao.searchGroupIdsByUserId(future).isEmpty(),
                "a future-dated membership must not grant anything yet",
            )
            assertTrue(
                authGroupUserDao.searchGroupIdsByUserId(revoked).isEmpty(),
                "a revoked membership survives only for the audit trail",
            )

            // The same window, judged from the other side and through the bulk cache loader.
            assertEquals(setOf(live, permanent), authGroupUserDao.searchUserIdsByGroupId(group))
            val byUser = authGroupUserDao.searchAllUserIdToGroupIdsForCache()
            assertTrue(byUser.containsKey(live))
            assertFalse(byUser.containsKey(lapsed))
            assertFalse(byUser.containsKey(revoked))
            assertEquals(
                setOf(live, permanent),
                authGroupUserDao.searchAllGroupIdToUserIdsForCache()[group]!!.toSet(),
            )
        } finally {
            ids.forEach { authGroupUserDao.deleteById(it) }
        }
    }

    /**
     * The counterpart contract: cache *invalidation* must see the rows resolution hides. Evicting a
     * user who did not need it costs a reload; missing the just-lapsed or future-dated member
     * leaves a stale permission set with nothing left to correct it.
     */
    @Test
    fun searchMemberUserIdsByGroupId_keepsEveryRowRegardlessOfWindow() {
        val group = "a1b2c3d4-0000-0000-0000-0000000000f1"
        val now = LocalDateTime.now()
        val live = uid("rlive")
        val lapsed = uid("rlapsed")
        val future = uid("rfuture")
        val revoked = uid("rrevoked")
        val ids = listOf(
            insertMembership(group, live),
            insertMembership(group, lapsed, start = now.minusDays(2), end = now.minusDays(1)),
            insertMembership(group, future, start = now.plusDays(1), end = now.plusDays(2)),
            insertMembership(group, revoked, revoked = true),
        )
        try {
            assertEquals(
                setOf(live, lapsed, future, revoked),
                authGroupUserDao.searchMemberUserIdsByGroupId(group),
            )
            assertEquals(setOf(live), authGroupUserDao.searchUserIdsByGroupId(group))
        } finally {
            ids.forEach { authGroupUserDao.deleteById(it) }
        }
    }

    /**
     * Both edges must be announced, and each exactly once. An absolute `end_time < now` query would
     * re-announce every membership that ever lapsed on every sweep — turning a cache eviction into
     * a permanent background load.
     */
    @Test
    fun searchMembershipsCrossingWindowBoundary_catchesBothEdgesOnce() {
        val group = "a1b2c3d4-0000-0000-0000-0000000000f2"
        val now = LocalDateTime.now()
        val since = now.minusMinutes(1)
        val justStarted = uid("estarted")
        val justEnded = uid("eended")
        val ids = listOf(
            insertMembership(group, justStarted, start = now.minusSeconds(30)),
            insertMembership(group, justEnded, end = now.minusSeconds(30)),
            insertMembership(group, uid("elapsed"), end = now.minusDays(30)),
            insertMembership(group, uid("elater"), start = now.plusDays(1)),
            insertMembership(group, uid("eperm")),
        )
        try {
            val crossed = authGroupUserDao.searchMembershipsCrossingWindowBoundary(since, now)
                .filter { it.groupId == group }
            assertEquals(setOf(justStarted, justEnded), crossed.map { it.userId }.toSet())
            assertEquals(crossed.size, crossed.distinctBy { it.id }.size, "no row announced twice")
        } finally {
            ids.forEach { authGroupUserDao.deleteById(it) }
        }
    }

    /**
     * `user_id` is CHAR(36), so a shorter value comes back space-padded and never equals the string
     * that was written. Test principals therefore have to be full-width, exactly like real UUIDs.
     */
    private fun uid(tag: String): String = "f0000000-0000-0000-0000-" + tag.padEnd(12, '0').take(12)

    private fun insertMembership(
        groupId: String,
        userId: String,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null,
        revoked: Boolean = false,
    ): String = authGroupUserDao.insert(AuthGroupUser.Companion().apply {
        this.groupId = groupId
        this.userId = userId
        this.startTime = start
        this.endTime = end
        this.revoked = revoked
    })

    @Test
    fun membershipDefaults_degradeToPreFeatureBehaviour() {
        val id = authGroupUserDao.insert(AuthGroupUser.Companion().apply {
            this.groupId = "a1b2c3d4-0000-0000-0000-000000000072"
            this.userId = "membership-defaults-user"
        })
        try {
            val row = authGroupUserDao.get(id)!!
            assertEquals("USER", row.principalType)
            assertEquals(false, row.revoked)
            assertNull(row.startTime, "no window means effective immediately")
            assertNull(row.endTime, "no window means never expires")
        } finally {
            authGroupUserDao.deleteById(id)
        }
    }
}
