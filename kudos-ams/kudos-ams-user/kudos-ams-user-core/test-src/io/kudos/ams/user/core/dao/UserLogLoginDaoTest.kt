package io.kudos.ams.user.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserLogLoginDao
 *
 * 测试数据来源：`UserLogLoginDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserLogLoginDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userLogLoginDao: UserLogLoginDao

}