package io.kudos.ability.distributed.tx.seata

import io.kudos.ability.distributed.tx.seata.main.Service
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test

/**
 * seata-AT模式测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerAvailable
@Import(Service::class)
open class AtSeataTest : SeataTestBase() {

    override fun txServiceGroup() = "other_tx_group"

    override fun dataSourceProxyMode() = "AT"

    override fun app1Port() = 28181

    override fun app2Port() = 28182

    override fun app1Name() = "ms12"

    override fun app2Name() = "ms22"

    @Test
    override fun localTx() = super.localTx()

    @Test
    override fun remoteTx() = super.remoteTx()

    companion object {
        @JvmStatic
        @DynamicPropertySource
        private fun changeProperties(registry: DynamicPropertyRegistry) {
            registry.add("seata.service.vgroup-mapping.other_tx_group") { "default" }
            registry.add("seata.tx-service-group") { "other_tx_group" }
            registry.add("seata.data-source-proxy-mode") { "AT" }
            startContainer(registry)
        }
    }

}
