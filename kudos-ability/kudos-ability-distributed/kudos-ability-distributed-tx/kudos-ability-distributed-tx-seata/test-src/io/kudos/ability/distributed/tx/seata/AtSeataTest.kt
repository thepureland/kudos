package io.kudos.ability.distributed.tx.seata

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.soul.ability.distributed.tx.seata.main.IFeignClient11
import org.springframework.context.annotation.Import
import java.util.function.Supplier

/**
 * seata-AT模式测试用例
 *
 * @author will
 * @since 5.1.1
 */
@SoulTest
@SpringBootApplication
@Import(IFeignClient11::class, IFeignClient21::class, Service::class)
@Disabled("避免与XaSeataTest一起跑时，造成其全局事务失效。但是单个跑都是没问题。")
class AtSeataTest : SeataTestBase() {
    override fun getTxServiceGroup(): String {
        return "other_tx_group"
    }

    override fun getDataSourceProxyMode(): String {
        return "AT"
    }

    override fun getApp1Port(): Int {
        return 28181
    }

    override fun getApp2Port(): Int {
        return 28182
    }

    override fun getApp1Name(): String {
        return "ms12"
    }

    override fun getApp2Name(): String {
        return "ms22"
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
            registry.add("seata.service.vgroup-mapping.other_tx_group", Supplier { "default" })
            registry.add("seata.tx-service-group", Supplier { "other_tx_group" })
            registry.add("seata.data-source-proxy-mode", Supplier { "AT" })
            SeataTestBase.Companion.registerProperties(registry)
        }
    }
}
