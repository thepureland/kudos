package io.kudos.ams.auth.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleUserDao
 *
 * 测试数据来源：`AuthRoleUserDaoTest.sql`
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
        // 测试存在的关系
        assertTrue(authRoleUserDao.exists("50000000-0000-0000-0000-000000000052", "50000000-0000-0000-0000-000000000050"))
        
        // 测试不存在的关系
        assertFalse(authRoleUserDao.exists("50000000-0000-0000-0000-000000000052", "non-existent-user"))
        assertFalse(authRoleUserDao.exists("non-existent-role", "50000000-0000-0000-0000-000000000050"))
    }
}