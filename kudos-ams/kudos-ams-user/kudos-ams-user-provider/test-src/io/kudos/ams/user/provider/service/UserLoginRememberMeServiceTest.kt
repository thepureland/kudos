package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IUserLoginRememberMeService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserLoginRememberMeService
 *
 * 测试数据来源：`UserLoginRememberMeServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserLoginRememberMeServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userLoginRememberMeService: IUserLoginRememberMeService

}
