package io.kudos.ams.user.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserContactWayDao
 *
 * 测试数据来源：`UserContactWayDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserContactWayDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userContactWayDao: UserContactWayDao

}
