package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysDictItemI18nDao
 *
 * 测试数据来源：`SysDictItemI18nDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemI18nDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysDictItemI18nDao: SysDictItemI18nDao

}
