package io.kudos.ms.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysResourceDao
 *
 * 测试数据来源：`SysResourceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysResourceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

}
