package io.kudos.ms.sys.api.admin.init

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import org.junit.jupiter.api.Disabled
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

/**
 * Manual smoke runner: boots the admin API app on port 8080 and blocks forever so a developer
 * can poke at it. NOT an automated test — the endless sleep would hang any CI/gradle test run
 * (it once stalled the whole multi-module test task), hence the [Disabled]. Remove the
 * annotation locally when you need a long-lived instance.
 */
@EnableKudosTest(
    classes = [SysApiAdminApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = ["server.port=8080"]
)
@EnabledIfDockerInstalled
@Disabled("Manual smoke runner: sleeps forever to keep the app up; hangs automated test runs.")
class SysApiAdminApplicationTest: RdbAndRedisCacheTestBase() {

    @Test
    fun test() {
        Thread.sleep(Long.MAX_VALUE)
    }

}