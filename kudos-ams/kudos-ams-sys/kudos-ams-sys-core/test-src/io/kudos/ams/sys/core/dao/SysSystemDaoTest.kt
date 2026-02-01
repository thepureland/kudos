package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysSystemDao
 *
 * 测试数据来源：`SysSystemDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSystemDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysSystemDao: SysSystemDao

}
