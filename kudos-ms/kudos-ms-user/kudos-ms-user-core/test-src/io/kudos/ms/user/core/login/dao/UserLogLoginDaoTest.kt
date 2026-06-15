package io.kudos.ms.user.core.login.dao

import io.kudos.ms.user.core.login.model.po.UserLogLogin
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for UserLogLoginDao — exercises the custom filter/count query methods directly against
 * the H2 fixture and round-trips a fully populated [UserLogLogin] entity (covering every column
 * binding / generated property accessor).
 *
 * Test data source: `UserLogLoginDaoTest.sql`.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserLogLoginDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userLogLoginDao: UserLogLoginDao

    private val tenantId = "user-tenant-dao-test-1-rEj1639e"
    private val userId1 = "588f6ca8-0000-0000-0000-000000000001"

    @Test
    fun searchByUserId() {
        val rows = userLogLoginDao.searchByUserId(userId1)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.userId == userId1 })
    }

    @Test
    fun searchByTenantId() {
        val rows = userLogLoginDao.searchByTenantId(tenantId)
        assertTrue(rows.size >= 3)
        assertTrue(rows.all { it.tenantId == tenantId })
    }

    @Test
    fun searchByFilters_appliesEachOptionalCondition() {
        val all = userLogLoginDao.searchByFilters(tenantId, null, null, null)
        assertTrue(all.size >= 3)

        val byUser = userLogLoginDao.searchByFilters(tenantId, userId1, null, null)
        assertTrue(byUser.isNotEmpty())
        assertTrue(byUser.all { it.userId == userId1 })

        // Time range covering "now" must include the CURRENT_TIMESTAMP fixtures.
        val start = LocalDateTime.now().minusDays(1)
        val end = LocalDateTime.now().plusDays(1)
        val ranged = userLogLoginDao.searchByFilters(tenantId, null, start, end)
        assertTrue(ranged.size >= 3)

        // A past-only window excludes the now-stamped rows.
        val pastOnly = userLogLoginDao.searchByFilters(
            tenantId, null, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(9)
        )
        assertTrue(pastOnly.isEmpty())
    }

    @Test
    fun countByFilters_matchesSearch() {
        val count = userLogLoginDao.countByFilters(tenantId, null, null, null)
        assertEquals(userLogLoginDao.searchByFilters(tenantId, null, null, null).size, count)
    }

    @Test
    fun countByLoginSuccess_splitsSuccessAndFailure() {
        val success = userLogLoginDao.countByLoginSuccess(true, tenantId, null, null, null)
        val failure = userLogLoginDao.countByLoginSuccess(false, tenantId, null, null, null)
        // Fixture: 2 success + 1 failure for this tenant.
        assertTrue(success >= 2)
        assertTrue(failure >= 1)
    }

    @Test
    fun crudRoundTrip_populatesAndReadsBackEveryColumn() {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now().withNano(0)
        val po = UserLogLogin {
            this.id = id
            this.userId = "dao-rt-user"
            this.username = "dao-rt-username"
            this.tenantId = "dao-rt-tenant"
            this.loginTime = now
            this.loginIp = 3232235777L // 192.168.1.1
            this.loginLocation = "Taipei"
            this.loginDevice = "PC"
            this.loginBrowser = "Chrome"
            this.loginOs = "Windows"
            this.userAgent = "Mozilla/5.0"
            this.loginSuccess = true
            this.failureReason = null
            this.sessionId = "sess-1"
            this.remark = "round-trip"
            this.createTime = now
        }
        userLogLoginDao.insert(po)

        val loaded = assertNotNull(userLogLoginDao.get(id))
        assertEquals("dao-rt-user", loaded.userId)
        assertEquals("dao-rt-username", loaded.username)
        assertEquals("dao-rt-tenant", loaded.tenantId)
        assertEquals(now, loaded.loginTime)
        assertEquals(3232235777L, loaded.loginIp)
        assertEquals("Taipei", loaded.loginLocation)
        assertEquals("PC", loaded.loginDevice)
        assertEquals("Chrome", loaded.loginBrowser)
        assertEquals("Windows", loaded.loginOs)
        assertEquals("Mozilla/5.0", loaded.userAgent)
        assertEquals(true, loaded.loginSuccess)
        assertEquals("sess-1", loaded.sessionId)
        assertEquals("round-trip", loaded.remark)
        assertEquals(now, loaded.createTime)

        // update + read back
        loaded.remark = "updated"
        loaded.loginSuccess = false
        userLogLoginDao.update(loaded)
        val reloaded = assertNotNull(userLogLoginDao.get(id))
        assertEquals("updated", reloaded.remark)
        assertEquals(false, reloaded.loginSuccess)

        // delete
        assertTrue(userLogLoginDao.deleteById(id))
        assertEquals(null, userLogLoginDao.get(id))
    }
}
