package io.kudos.ability.distributed.tx.seata

import io.kudos.ability.distributed.tx.seata.main.Service
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.junit.jupiter.api.Disabled
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test

/**
 * seata-XA模式测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(Service::class)
@Disabled("autocommit必须要为true")
open class XaSeataTest : SeataTestBase() {

    override fun txServiceGroup() = "default_tx_group"

    override fun dataSourceProxyMode() = "XA"

    override fun app1Port() = 28183

    override fun app2Port() = 28184

    override fun app1Name() = "ms11"

    override fun app2Name() = "ms21"

    @Test
    override fun localTx() = super.localTx()

    @Test
    override fun remoteTx() = super.remoteTx()

    companion object {
        @JvmStatic
        @DynamicPropertySource
        private fun changeProperties(registry: DynamicPropertyRegistry) {
            registry.add("seata.service.vgroup-mapping.default_tx_group") { "default" }
            registry.add("seata.tx-service-group") { "default_tx_group" }
            registry.add("seata.data-source-proxy-mode") { "XA" }
            startContainer(registry)
        }
    }

}
