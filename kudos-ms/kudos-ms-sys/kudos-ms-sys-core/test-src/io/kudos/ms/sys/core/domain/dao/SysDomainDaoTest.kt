package io.kudos.ms.sys.core.domain.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysDomainDao
 *
 * Test data source: `SysDomainDaoTest.sql`
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
