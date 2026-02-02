package io.kudos.ms.user.core.service

import io.kudos.ms.user.core.service.iservice.IUserContactWayService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserContactWayService
 *
 * 测试数据来源：`UserContactWayServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserContactWayServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userContactWayService: IUserContactWayService

}
