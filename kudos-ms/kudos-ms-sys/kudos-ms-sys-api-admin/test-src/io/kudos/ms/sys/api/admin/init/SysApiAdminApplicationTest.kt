package io.kudos.ms.sys.api.admin.init

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@EnableKudosTest(
    classes = [SysApiAdminApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = ["server.port=8080"]
)
@EnabledIfDockerInstalled
class SysApiAdminApplicationTest: RdbAndRedisCacheTestBase() {

    @Test
    fun test() {
        Thread.sleep(Long.MAX_VALUE)
    }

}