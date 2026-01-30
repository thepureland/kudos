package io.kudos.ams.auth.provider.service

import io.kudos.ams.auth.provider.service.iservice.IAuthGroupService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource

/**
 * junit test for AuthGroupService
 *
 * 测试数据来源：`AuthGroupServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authGroupService: IAuthGroupService

}
