package io.kudos.ms.auth.core.role.temporal.service

import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleUserTemporalService.
 *
 * Test data source: `AuthRoleUserTemporalServiceTest.sql`
 * user1 permanently holds role1 (baseline); the rest is exercised via bindTemporal with windows
 * computed relative to now() so the assertions are deterministic regardless of run time.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleUserTemporalServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthRoleUserTemporalService

    @Resource
    private lateinit var dao: AuthRoleUserDao

    private val role1 = "7e3b9a01-0000-0000-0000-0000000000a1"
    private val role2 = "7e3b9a01-0000-0000-0000-0000000000a2"
    private val user1 = "7e3b9a01-0000-0000-0000-0000000000b1"
    private val user2 = "7e3b9a01-0000-0000-0000-0000000000b2"

    @Test
    fun permanentGrant_isActive() {
        // Fixture: user1 permanently holds role1 (no window).
        assertTrue(dao.searchRoleIdsByUserId(user1).contains(role1))
    }

    @Test
    fun bindTemporal_currentWindow_isActive() {
        val now = LocalDateTime.now()
        service.bindTemporal(role2, user2, now.minusDays(1), now.plusDays(1))
        assertTrue(dao.searchRoleIdsByUserId(user2).contains(role2))
    }

    @Test
    fun bindTemporal_pastWindow_excluded() {
        val now = LocalDateTime.now()
        service.bindTemporal(role2, user2, now.minusDays(2), now.minusDays(1))
        assertFalse(dao.searchRoleIdsByUserId(user2).contains(role2))
    }

    @Test
    fun bindTemporal_futureWindow_excluded() {
        val now = LocalDateTime.now()
        service.bindTemporal(role2, user2, now.plusDays(1), now.plusDays(2))
        assertFalse(dao.searchRoleIdsByUserId(user2).contains(role2))
    }

    @Test
    fun bindTemporal_openEndedFutureStart_excludedUntilStart() {
        val now = LocalDateTime.now()
        // No end, but starts tomorrow → not yet active.
        service.bindTemporal(role2, user2, now.plusDays(1), null)
        assertFalse(dao.searchRoleIdsByUserId(user2).contains(role2))
    }

    @Test
    fun bindTemporal_startAfterEnd_rejected() {
        val now = LocalDateTime.now()
        assertFailsWith<IllegalArgumentException> {
            service.bindTemporal(role2, user2, now.plusDays(2), now.plusDays(1))
        }
    }

    @Test
    fun bindTemporal_replaceSemantics_singleRow() {
        val now = LocalDateTime.now()
        // First an expired window, then re-bind permanent — the old row must be replaced, not added.
        service.bindTemporal(role2, user2, now.minusDays(2), now.minusDays(1))
        service.bindTemporal(role2, user2, null, null)
        assertTrue(dao.searchRoleIdsByUserId(user2).contains(role2))
        assertEquals(1, dao.searchUserIdsByRoleId(role2).count { it == user2 })
    }

    @Test
    fun purgeExpired_deletesExpiredKeepsActive() {
        val now = LocalDateTime.now()
        service.bindTemporal(role1, user2, now.minusDays(2), now.minusDays(1)) // expired
        service.bindTemporal(role2, user2, now.minusDays(1), now.plusDays(1))  // active
        val purged = service.purgeExpired()
        assertTrue(purged >= 1)
        val active = dao.searchRoleIdsByUserId(user2)
        assertTrue(active.contains(role2))
        assertFalse(active.contains(role1))
    }
}
