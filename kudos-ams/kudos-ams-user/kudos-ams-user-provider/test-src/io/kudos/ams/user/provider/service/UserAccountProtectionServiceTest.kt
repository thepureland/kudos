package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IUserAccountProtectionService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserAccountProtectionService
 *
 * 测试数据来源：`UserAccountProtectionServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountProtectionServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userAccountProtectionService: IUserAccountProtectionService

}
