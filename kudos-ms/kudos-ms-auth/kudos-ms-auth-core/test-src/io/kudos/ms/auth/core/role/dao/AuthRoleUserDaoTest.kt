package io.kudos.ms.auth.core.role.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleUserDao
 *
 * Test data source: `AuthRoleUserDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(authRoleUserDao.exists("42d84639-0000-0000-0000-000000000052", "42d84639-0000-0000-0000-000000000050"))

        // Test a non-existent relation
        assertFalse(authRoleUserDao.exists("42d84639-0000-0000-0000-000000000052", "non-existent-user"))
        assertFalse(authRoleUserDao.exists("non-existent-role", "42d84639-0000-0000-0000-000000000050"))
    }
}