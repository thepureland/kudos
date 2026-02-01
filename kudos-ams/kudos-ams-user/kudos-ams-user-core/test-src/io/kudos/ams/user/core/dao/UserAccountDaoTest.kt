package io.kudos.ams.user.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserAccountDao
 *
 * 测试数据来源：`UserAccountDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userAccountDao: UserAccountDao

}