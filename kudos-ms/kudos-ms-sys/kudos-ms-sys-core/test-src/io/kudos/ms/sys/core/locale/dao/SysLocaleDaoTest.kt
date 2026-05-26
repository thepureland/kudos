package io.kudos.ms.sys.core.locale.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource


/**
 * junit test for SysLocaleDao
 *
 * Test data source: `SysLocaleDaoTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysLocaleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysLocaleDao: SysLocaleDao

}
