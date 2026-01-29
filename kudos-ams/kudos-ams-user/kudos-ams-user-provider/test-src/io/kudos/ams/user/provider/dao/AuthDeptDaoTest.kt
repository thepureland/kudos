package io.kudos.ams.user.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for AuthDeptDao
 *
 * 测试数据来源：`AuthDeptDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthDeptDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authDeptDao: AuthDeptDao

}