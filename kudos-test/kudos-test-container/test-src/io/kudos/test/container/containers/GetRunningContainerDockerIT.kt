package io.kudos.test.container.containers

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import kotlin.test.Test

/**
 * Docker-gated sweep exercising the daemon query path (`getRunningContainer()`) of every container
 * object without pulling or starting any heavy images.
 *
 * The result is not asserted to be null: parallel builds on the same agent may legitimately have one
 * of these labels running. The point is to drive the line and confirm the call completes without
 * throwing against the real daemon.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
internal class GetRunningContainerDockerIT {

    @Test
    fun `every container getRunningContainer queries the daemon without throwing`() {
        // each call returns either null or a Container; both are fine, we only need it not to throw
        val probes: List<() -> Any?> = listOf(
            { RedisTestContainer.getRunningContainer() },
            { H2TestContainer.getRunningContainer() },
            { KafkaTestContainer.getRunningContainer() },
            { MySqlTestContainer.getRunningContainer() },
            { MongoTestContainer.getRunningContainer() },
            { RabbitMqTestContainer.getRunningContainer() },
            { MinioTestContainer.getRunningContainer() },
            { ClickHouseTestContainer.getRunningContainer() },
            { InfluxdbTestContainer.getRunningContainer() },
            { NacosTestContainer.getRunningContainer() },
            { RocketMqTestContainer.getRunningContainer() },
            { SeataTestContainer.getRunningContainer() },
            { SmtpTestContainer.getRunningContainer() },
            { WireMockTestContainer.getRunningContainer() },
            { PostgresTestContainer.getRunningContainer() },
        )
        probes.forEach { it() }
    }
}
