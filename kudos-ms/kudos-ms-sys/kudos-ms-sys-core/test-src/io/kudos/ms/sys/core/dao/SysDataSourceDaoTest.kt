package io.kudos.ms.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysDataSourceDao
 *
 * 测试数据来源：`SysDataSourceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDataSourceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysDataSourceDao: SysDataSourceDao

}
