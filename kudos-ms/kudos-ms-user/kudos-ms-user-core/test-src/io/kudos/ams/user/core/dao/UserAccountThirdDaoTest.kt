package io.kudos.ms.user.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserAccountThirdDao
 *
 * 测试数据来源：`UserAccountThirdDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountThirdDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userAccountThirdDao: UserAccountThirdDao

}
