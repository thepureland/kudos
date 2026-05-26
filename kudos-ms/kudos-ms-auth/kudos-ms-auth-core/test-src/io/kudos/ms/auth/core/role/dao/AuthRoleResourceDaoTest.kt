package io.kudos.ms.auth.core.role.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleResourceDao
 *
 * Test data source: `AuthRoleResourceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleResourceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(authRoleResourceDao.exists("49748162-0000-0000-0000-000000000060", "49748162-0000-0000-0000-000000000062"))

        // Test a non-existent relation
        assertFalse(authRoleResourceDao.exists("49748162-0000-0000-0000-000000000060", "non-existent-resource"))
        assertFalse(authRoleResourceDao.exists("non-existent-role", "49748162-0000-0000-0000-000000000062"))
    }

    @Test
    fun searchRoleIdsByResourceId() {
        val resourceId = "49748162-0000-0000-0000-000000000062"
        val roleIds = authRoleResourceDao.searchRoleIdsByResourceId(resourceId)
        assertTrue(roleIds.isNotEmpty())
        assertTrue(roleIds.contains("49748162-0000-0000-0000-000000000060"))
        
        // Test a non-existent resource ID
        val emptyRoleIds = authRoleResourceDao.searchRoleIdsByResourceId("non-existent-resource")
        assertTrue(emptyRoleIds.isEmpty())
    }
}