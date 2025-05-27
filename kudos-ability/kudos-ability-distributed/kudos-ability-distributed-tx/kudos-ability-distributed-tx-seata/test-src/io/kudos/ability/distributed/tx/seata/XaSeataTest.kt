package io.kudos.ability.distributed.tx.seata

import org.junit.jupiter.api.Test
import org.soul.ability.distributed.tx.seata.main.IFeignClient12
import org.springframework.context.annotation.Import
import java.util.function.Supplier

/**
 * seata-XA模式测试用例
 *
 * @author will
 * @since 5.1.1
 */
@SoulTest
@SpringBootApplication
@Import(IFeignClient12::class, IFeignClient22::class, Service::class)
class XaSeataTest : SeataTestBase() {
    override fun getTxServiceGroup(): String {
        return "default_tx_group"
    }

    override fun getDataSourceProxyMode(): String {
        return "XA"
    }

    override fun getApp1Port(): Int {
        return 28183
    }

    override fun getApp2Port(): Int {
        return 28184
    }

    override fun getApp1Name(): String {
        return "ms11"
    }

    override fun getApp2Name(): String {
        return "ms21"
    }

    @Test
    override fun localTx() {
        super.localTx()
    }

    @Test
    override fun remoteTx() {
        super.remoteTx()
    }

    companion object {
        @DynamicPropertySource
        private fun changeProperties(registry: DynamicPropertyRegistry) {
            registry.add("seata.service.vgroup-mapping.default_tx_group", Supplier { "default" })
            registry.add("seata.tx-service-group", Supplier { "default_tx_group" })
            registry.add("seata.data-source-proxy-mode", Supplier { "XA" })
            SeataTestBase.Companion.registerProperties(registry)
        }
    }
}
