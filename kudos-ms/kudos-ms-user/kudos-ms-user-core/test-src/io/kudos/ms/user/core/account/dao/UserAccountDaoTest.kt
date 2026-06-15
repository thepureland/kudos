package io.kudos.ms.user.core.account.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for UserAccountDao — covers the custom query methods (`getUsersByTenantIdAndUsername`,
 * `searchActiveUsersForCache`, `searchActiveUserIdsByTenantId`) against the H2 fixture, including the
 * active=false filtering and not-found paths.
 *
 * Test data source: `UserAccountDaoTest.sql`.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    private val tenant1 = "user-tenant-dao-test-1-Y6hpgGno"
    private val tenant2 = "user-tenant-dao-test-2-Y6hpgGno"

    @Test
    fun getUsersByTenantIdAndUsername() {
        val found = userAccountDao.getUsersByTenantIdAndUsername(tenant1, "user-account-dao-test-1-Y6hpgGno")
        assertNotNull(found)
        assertEquals("c07f7328-0000-0000-0000-000000000001", found.id)

        // Wrong tenant / username -> null.
        assertNull(userAccountDao.getUsersByTenantIdAndUsername(tenant1, "no-such-user"))
        assertNull(userAccountDao.getUsersByTenantIdAndUsername("no-such-tenant", "user-account-dao-test-1-Y6hpgGno"))
    }

    @Test
    fun searchActiveUsersForCache() {
        val active = userAccountDao.searchActiveUsersForCache()
        // The inactive fixture row (id ...0003, active=false) must be excluded.
        assertTrue(active.none { it.id == "c07f7328-0000-0000-0000-000000000003" })
        assertTrue(active.any { it.id == "c07f7328-0000-0000-0000-000000000001" })
    }

    @Test
    fun searchActiveUserIdsByTenantId() {
        val tenant1Ids = userAccountDao.searchActiveUserIdsByTenantId(tenant1)
        assertTrue(tenant1Ids.contains("c07f7328-0000-0000-0000-000000000001"))
        assertTrue(tenant1Ids.contains("c07f7328-0000-0000-0000-000000000002"))

        // tenant2 only has an inactive user -> filtered out.
        val tenant2Ids = userAccountDao.searchActiveUserIdsByTenantId(tenant2)
        assertTrue(tenant2Ids.none { it == "c07f7328-0000-0000-0000-000000000003" })
    }
}
