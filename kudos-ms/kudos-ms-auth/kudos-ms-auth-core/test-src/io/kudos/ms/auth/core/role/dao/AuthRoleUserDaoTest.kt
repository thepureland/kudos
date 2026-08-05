package io.kudos.ms.auth.core.role.dao

import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
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
 * junit test for [AuthRoleUserDao].
 *
 * Beyond exists, covers the temporal-window contract that drives permission resolution:
 *  - searchRoleIdsByUserId / searchAllUserIdToRoleIdsForCache exclude out-of-window grants and
 *    always include permanent (NULL/NULL) ones — evaluated at an explicit, fixed `now` so the
 *    assertions never depend on the wall clock.
 *  - searchExpiredGrants returns only rows whose end_time has passed (NULL end excluded).
 * Also covers searchUserIdsByRoleId, searchGrantsByRoleId, getAllRoleIdToUserIdsForCache,
 * searchByRoleIdAndUserId, and the two delete paths (rolled back with the test transaction).
 *
 * Test data source: `AuthRoleUserDaoTest.sql`
 *  - permanent: role52 -> {user50, user51}
 *  - temporal (reference instant 2025-06-15 12:00): role53 -> user50 expired(Jan~Mar),
 *    user51 not-yet(Dec~Jan), user52 currently-active(Jun~Jul)
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    private val role52 = "42d84639-0000-0000-0000-000000000052"
    private val role53 = "42d84639-0000-0000-0000-000000000053"
    private val user50 = "42d84639-0000-0000-0000-000000000050"
    private val user51 = "42d84639-0000-0000-0000-000000000051"
    private val user52 = "42d84639-0000-0000-0000-000000000056"

    /** A reference instant inside user52's window, after user50's, before user51's. */
    private val now = LocalDateTime.of(2025, 6, 15, 12, 0, 0)

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(authRoleUserDao.exists(role52, user50))

        // Test a non-existent relation
        assertFalse(authRoleUserDao.exists(role52, "non-existent-user"))
        assertFalse(authRoleUserDao.exists("non-existent-role", user50))
    }

    /**
     * A grant carries delegation and revocation state, not just the (role, user) pair. This pins the
     * mapping of those columns; the behaviour built on them lands with the delegation feature.
     */
    @Test
    fun grantGovernanceColumns_roundTrip() {
        val parentId = authRoleUserDao.insert(AuthRoleUser.Companion().apply {
            this.roleId = "42d84639-0000-0000-0000-000000000052"
            this.userId = "gov-upstream-user"
            this.delegableDepth = 2
        })
        val childId = authRoleUserDao.insert(AuthRoleUser.Companion().apply {
            this.roleId = "42d84639-0000-0000-0000-000000000052"
            this.userId = "gov-downstream-user"
            this.principalType = "SERVICE"
            this.grantedBy = "gov-upstream-user"
            this.parentGrantId = parentId
            this.delegableDepth = 1
            this.scopeSnapshot = """{"org":["org-1"]}"""
            this.revoked = false
            this.revokeReason = null
        })
        try {
            val child = authRoleUserDao.get(childId)!!
            assertEquals("SERVICE", child.principalType)
            assertEquals("gov-upstream-user", child.grantedBy)
            assertEquals(parentId, child.parentGrantId, "the chain edge must survive the round trip")
            assertEquals(1, child.delegableDepth, "the delegated grant narrows the parent's depth of 2")
            assertEquals("""{"org":["org-1"]}""", child.scopeSnapshot)
            assertEquals(false, child.revoked)

            // Defaults must degrade to the pre-delegation behaviour: a plain grant is terminal and live.
            val parent = authRoleUserDao.get(parentId)!!
            assertEquals("USER", parent.principalType)
            assertEquals(false, parent.revoked)
            assertNull(parent.parentGrantId)
        } finally {
            authRoleUserDao.deleteById(childId)
            authRoleUserDao.deleteById(parentId)
        }
    }

    @Test
    fun searchRoleIdsByUserId_permanentAlwaysActive() {
        // user50 holds role52 permanently (active at any now) and role53 only Jan~Mar (expired at `now`).
        val atNow = authRoleUserDao.searchRoleIdsByUserId(user50, now)
        assertTrue(atNow.contains(role52), "permanent grant must be active")
        assertFalse(atNow.contains(role53), "expired temporal grant must be excluded at $now")
    }

    @Test
    fun searchRoleIdsByUserId_notYetActiveExcluded() {
        // user51 holds role52 permanently and role53 only Dec~Jan (not yet active at `now`).
        val atNow = authRoleUserDao.searchRoleIdsByUserId(user51, now)
        assertTrue(atNow.contains(role52))
        assertFalse(atNow.contains(role53), "not-yet-effective grant must be excluded at $now")
    }

    @Test
    fun searchRoleIdsByUserId_withinWindowIncluded() {
        // user52 holds only role53, window Jun~Jul, active at `now`.
        val atNow = authRoleUserDao.searchRoleIdsByUserId(user52, now)
        assertEquals(listOf(role53), atNow)
        // Outside the window the same grant disappears.
        val before = authRoleUserDao.searchRoleIdsByUserId(user52, LocalDateTime.of(2025, 1, 1, 0, 0))
        assertTrue(before.isEmpty(), "grant should be inactive before its start_time")
        val after = authRoleUserDao.searchRoleIdsByUserId(user52, LocalDateTime.of(2025, 8, 1, 0, 0))
        assertTrue(after.isEmpty(), "grant should be inactive after its end_time")
    }

    @Test
    fun searchAllUserIdToRoleIdsForCache_respectsWindows() {
        val map = authRoleUserDao.searchAllUserIdToRoleIdsForCache(now)
        // user52's only grant is active at `now`.
        assertEquals(listOf(role53), map[user52])
        // user50: permanent role52 active, expired role53 excluded.
        assertTrue(map[user50]?.contains(role52) == true)
        assertFalse(map[user50]?.contains(role53) == true)
        // user51: permanent role52 active, not-yet role53 excluded.
        assertTrue(map[user51]?.contains(role52) == true)
        assertFalse(map[user51]?.contains(role53) == true)
    }

    @Test
    fun searchExpiredGrants() {
        val expired = authRoleUserDao.searchExpiredGrants(now)
        val ids = expired.map { it.id }.toSet()
        // user50's role53 grant ends 2025-03-01 (< now) -> expired.
        assertTrue(ids.contains("42d84639-0000-0000-0000-000000000060"), "ended grant must be returned")
        // user51's role53 grant ends 2026-01-01 (> now) -> not expired.
        assertFalse(ids.contains("42d84639-0000-0000-0000-000000000061"))
        // user52's role53 grant ends 2025-07-01 (> now) -> not expired.
        assertFalse(ids.contains("42d84639-0000-0000-0000-000000000062"))
        // permanent grants (NULL end_time) are never expired.
        assertFalse(ids.contains("42d84639-0000-0000-0000-000000000054"))
    }

    @Test
    fun temporalQueries_defaultNowOverloads() {
        // Exercise the default-parameter (now = LocalDateTime.now()) overloads. user50 holds role52
        // permanently, so it is active regardless of the current wall-clock instant.
        assertTrue(authRoleUserDao.searchRoleIdsByUserId(user50).contains(role52))
        assertTrue(authRoleUserDao.searchAllUserIdToRoleIdsForCache()[user50]?.contains(role52) == true)
        // searchExpiredGrants with the default now: user50's role53 grant ended 2025-03-01, which is
        // in the past relative to any realistic test clock (>= 2025), so it is reported expired.
        val expiredIds = authRoleUserDao.searchExpiredGrants().map { it.id }.toSet()
        assertTrue(expiredIds.contains("42d84639-0000-0000-0000-000000000060"))
    }

    @Test
    fun searchUserIdsByRoleId() {
        // role52 has both permanent users; searchUserIdsByRoleId ignores time windows.
        assertEquals(setOf(user50, user51), authRoleUserDao.searchUserIdsByRoleId(role52).toSet())
        // role53 has all three users regardless of window.
        assertEquals(setOf(user50, user51, user52), authRoleUserDao.searchUserIdsByRoleId(role53).toSet())
        assertTrue(authRoleUserDao.searchUserIdsByRoleId("non-existent-role").isEmpty())
    }

    @Test
    fun searchGrantsByRoleId() {
        // role53 returns raw rows including window fields, regardless of validity.
        val grants = authRoleUserDao.searchGrantsByRoleId(role53)
        assertEquals(3, grants.size)
        val byUser = grants.associateBy { it.userId }
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0), byUser[user52]?.startTime)
        assertEquals(LocalDateTime.of(2025, 7, 1, 0, 0), byUser[user52]?.endTime)
        // permanent grants carry null window fields.
        val permanent = authRoleUserDao.searchGrantsByRoleId(role52).first { it.userId == user50 }
        assertTrue(permanent.startTime == null && permanent.endTime == null)
    }

    @Test
    fun getAllRoleIdToUserIdsForCache() {
        val map = authRoleUserDao.getAllRoleIdToUserIdsForCache()
        assertEquals(setOf(user50, user51), map[role52]?.toSet())
        // role53 groups all three users regardless of window (raw grouping, no time filter).
        assertEquals(setOf(user50, user51, user52), map[role53]?.toSet())
    }

    @Test
    fun searchByRoleIdAndUserId() {
        val rows = authRoleUserDao.searchByRoleIdAndUserId(role53, user52)
        assertEquals(1, rows.size)
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0), rows.first().startTime)
        // no match -> empty
        assertTrue(authRoleUserDao.searchByRoleIdAndUserId(role53, "non-existent-user").isEmpty())
    }

    @Test
    fun deleteByRoleIdAndUserId() {
        assertTrue(authRoleUserDao.exists(role52, user50))
        assertEquals(1, authRoleUserDao.deleteByRoleIdAndUserId(role52, user50))
        assertFalse(authRoleUserDao.exists(role52, user50))
        // the other user of role52 survives
        assertTrue(authRoleUserDao.exists(role52, user51))
        // deleting a non-existent relation removes nothing
        assertEquals(0, authRoleUserDao.deleteByRoleIdAndUserId(role52, "non-existent-user"))
    }

    @Test
    fun deleteByRoleId() {
        // role53 has three grant rows
        assertEquals(3, authRoleUserDao.deleteByRoleId(role53))
        assertTrue(authRoleUserDao.searchUserIdsByRoleId(role53).isEmpty())
        // role52 untouched
        assertEquals(2, authRoleUserDao.searchUserIdsByRoleId(role52).size)
        // deleting for a role with no grants removes nothing
        assertEquals(0, authRoleUserDao.deleteByRoleId("non-existent-role"))
    }
}
