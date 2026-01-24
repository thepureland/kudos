package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysPortalDao
 *
 * 测试数据来源：`SysPortalDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysPortalDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysPortalDao: SysPortalDao

}
