package io.kudos.ms.sys.core.outline.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource


/**
 * junit test for SysOutLineDao
 *
 * Test data source: `SysOutLineDaoTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysOutLineDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysOutLineDao: SysOutLineDao

}
