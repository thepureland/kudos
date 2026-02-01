package io.kudos.ams.auth.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for AuthGroupDao
 *
 * 测试数据来源：`AuthGroupDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

}
