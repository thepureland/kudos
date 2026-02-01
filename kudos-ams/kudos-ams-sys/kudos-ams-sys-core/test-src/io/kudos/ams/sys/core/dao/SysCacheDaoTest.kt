package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysCacheDao
 *
 * 测试数据来源：`SysCacheDaoTest.sql`
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
