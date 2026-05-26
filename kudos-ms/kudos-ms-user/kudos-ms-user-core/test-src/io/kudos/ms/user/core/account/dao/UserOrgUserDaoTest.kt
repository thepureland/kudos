package io.kudos.ms.user.core.account.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for UserOrgUserDao
 *
 * Test data source: `UserOrgUserDaoTest.sql`.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserOrgUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Test
    fun exists() {
        // Test an existing relation.
        assertTrue(userOrgUserDao.exists("952bb1b3-0000-0000-0000-000000000042", "952bb1b3-0000-0000-0000-000000000040"))

        // Test a non-existent relation.
        assertFalse(userOrgUserDao.exists("952bb1b3-0000-0000-0000-000000000042", "non-existent-user"))
        assertFalse(userOrgUserDao.exists("non-existent-org", "952bb1b3-0000-0000-0000-000000000040"))
    }
}