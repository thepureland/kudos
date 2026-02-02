package io.kudos.ms.user.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserLoginRememberMeDao
 *
 * 测试数据来源：`UserLoginRememberMeDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserLoginRememberMeDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userLoginRememberMeDao: UserLoginRememberMeDao

}
