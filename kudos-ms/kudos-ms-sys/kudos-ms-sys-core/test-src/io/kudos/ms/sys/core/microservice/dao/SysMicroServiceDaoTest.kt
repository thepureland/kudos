package io.kudos.ms.sys.core.microservice.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysMicroServiceDao
 *
 * Test data source: `SysMicroServiceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysMicroServiceDao: SysMicroServiceDao

    // This DAO has no custom methods; only basic CRUD functionality (provided by parent class) is tested
}
