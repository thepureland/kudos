package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysI18nDao
 *
 * 测试数据来源：`SysI18nDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysI18nDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysI18nDao: SysI18nDao

}
