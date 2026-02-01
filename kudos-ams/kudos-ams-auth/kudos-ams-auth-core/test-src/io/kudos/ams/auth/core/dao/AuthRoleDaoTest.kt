package io.kudos.ams.auth.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for AuthRoleDao
 *
 * 测试数据来源：`AuthRoleDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleDao: io.kudos.ams.auth.core.dao.AuthRoleDao

}