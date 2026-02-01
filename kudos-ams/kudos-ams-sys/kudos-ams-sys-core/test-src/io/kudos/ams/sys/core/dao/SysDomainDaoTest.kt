package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysDomainDao
 *
 * 测试数据来源：`SysDomainDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDomainDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysDomainDao: SysDomainDao

}
