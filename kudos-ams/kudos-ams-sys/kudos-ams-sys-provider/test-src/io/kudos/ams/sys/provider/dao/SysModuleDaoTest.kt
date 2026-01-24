package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysModuleDao
 *
 * 测试数据来源：`SysModuleDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysModuleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysModuleDao: SysModuleDao

    // 此Dao类没有自定义方法，只测试基础CRUD功能（由父类提供）
}
