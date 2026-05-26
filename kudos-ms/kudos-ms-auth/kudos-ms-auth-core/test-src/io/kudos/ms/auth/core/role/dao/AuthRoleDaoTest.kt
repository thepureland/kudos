package io.kudos.ms.auth.core.role.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for AuthRoleDao
 *
 * Test data source: `AuthRoleDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

}