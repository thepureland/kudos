package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictItemService
 *
 * 测试数据来源：`SysDictItemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictItemService: ISysDictItemService


}
