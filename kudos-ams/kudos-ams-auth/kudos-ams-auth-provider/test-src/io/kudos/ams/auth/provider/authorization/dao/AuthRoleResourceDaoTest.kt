package io.kudos.ams.auth.provider.authorization.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleResourceDao
 *
 * 测试数据来源：`AuthRoleResourceDaoTest.sql`
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
        // 测试存在的关系
        assertTrue(authRoleResourceDao.exists("50000000-0000-0000-0000-000000000060", "50000000-0000-0000-0000-000000000062"))
        
        // 测试不存在的关系
        assertFalse(authRoleResourceDao.exists("50000000-0000-0000-0000-000000000060", "non-existent-resource"))
        assertFalse(authRoleResourceDao.exists("non-existent-role", "50000000-0000-0000-0000-000000000062"))
    }

    @Test
    fun searchRoleIdsByResourceId() {
        val resourceId = "50000000-0000-0000-0000-000000000062"
        val roleIds = authRoleResourceDao.searchRoleIdsByResourceId(resourceId)
        assertTrue(roleIds.size >= 1)
        assertTrue(roleIds.contains("50000000-0000-0000-0000-000000000060"))
        
        // 测试不存在的资源ID
        val emptyRoleIds = authRoleResourceDao.searchRoleIdsByResourceId("non-existent-resource")
        assertTrue(emptyRoleIds.isEmpty())
    }
}