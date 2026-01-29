package io.kudos.ams.user.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for AuthLogLoginDao
 *
 * 测试数据来源：`AuthLogLoginDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthLogLoginDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authLogLoginDao: AuthLogLoginDao

}