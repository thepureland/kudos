package io.kudos.ams.user.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserAccountProtectionDao
 *
 * 测试数据来源：`UserAccountProtectionDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountProtectionDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userAccountProtectionDao: UserAccountProtectionDao

}
