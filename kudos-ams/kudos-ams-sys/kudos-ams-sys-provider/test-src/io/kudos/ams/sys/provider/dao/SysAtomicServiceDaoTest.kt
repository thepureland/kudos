package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysAtomicServiceDao
 *
 * 测试数据来源：`SysAtomicServiceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAtomicServiceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysAtomicServiceDao: SysAtomicServiceDao

}
