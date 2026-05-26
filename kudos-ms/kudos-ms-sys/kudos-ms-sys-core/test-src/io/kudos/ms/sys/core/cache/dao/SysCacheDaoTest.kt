package io.kudos.ms.sys.core.cache.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysCacheDao
 *
 * Test data source: `SysCacheDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysCacheDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysCacheDao: SysCacheDao

}
