package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysTenantDao
 *
 * 测试数据来源：`SysTenantDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysTenantDao: SysTenantDao

}
