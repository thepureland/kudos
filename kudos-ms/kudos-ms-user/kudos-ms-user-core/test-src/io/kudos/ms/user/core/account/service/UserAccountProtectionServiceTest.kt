package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.service.iservice.IUserAccountProtectionService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource

/**
 * junit test for UserAccountProtectionService
 *
 * Test data source: `UserAccountProtectionServiceTest.sql`.
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
