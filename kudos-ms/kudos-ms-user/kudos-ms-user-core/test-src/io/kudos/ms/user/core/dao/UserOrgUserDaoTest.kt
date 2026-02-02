package io.kudos.ms.user.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for UserOrgUserDao
 *
 * 测试数据来源：`UserOrgUserDaoTest.sql`
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
        // 测试存在的关系
        assertTrue(userOrgUserDao.exists("952bb1b3-0000-0000-0000-000000000042", "952bb1b3-0000-0000-0000-000000000040"))
        
        // 测试不存在的关系
        assertFalse(userOrgUserDao.exists("952bb1b3-0000-0000-0000-000000000042", "non-existent-user"))
        assertFalse(userOrgUserDao.exists("non-existent-org", "952bb1b3-0000-0000-0000-000000000040"))
    }
}