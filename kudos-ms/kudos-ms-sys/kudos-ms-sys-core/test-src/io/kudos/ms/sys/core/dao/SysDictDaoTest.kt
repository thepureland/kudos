package io.kudos.ms.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource

/**
 * junit test for SysDictDao
 *
 * 测试数据来源：`SysDictDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysDictDao: SysDictDao

}
