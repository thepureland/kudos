package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysMicroServiceDao
 *
 * 测试数据来源：`SysMicroServiceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysMicroServiceDao: SysMicroServiceDao

    // 此Dao类没有自定义方法，只测试基础CRUD功能（由父类提供）
}
