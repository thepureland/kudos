package io.kudos.ams.auth.provider.user.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthDeptUserDao
 *
 * 测试数据来源：`AuthDeptUserDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthDeptUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authDeptUserDao: AuthDeptUserDao

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(authDeptUserDao.exists("952bb1b3-0000-0000-0000-000000000042", "952bb1b3-0000-0000-0000-000000000040"))
        
        // 测试不存在的关系
        assertFalse(authDeptUserDao.exists("952bb1b3-0000-0000-0000-000000000042", "non-existent-user"))
        assertFalse(authDeptUserDao.exists("non-existent-dept", "952bb1b3-0000-0000-0000-000000000040"))
    }
}