package io.kudos.ability.distributed.tx.seata

import io.kudos.ability.distributed.tx.seata.main.Service
import io.kudos.context.kit.SpringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import javax.sql.DataSource
import kotlin.test.Test

/**
 * seata-AT模式测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(Service::class, io.kudos.ability.distributed.tx.seata.feign.SeataFeignXidConfig::class)
open class AtSeataTest : SeataTestBase() {

    override fun txServiceGroup() = "other_tx_group"

    override fun dataSourceProxyMode() = "AT"

    override fun app1Port() = 28181

    override fun app2Port() = 28182

    override fun app1Name() = "ms12"

    override fun app2Name() = "ms22"

    @Test
    fun datasource() {
        val ds = SpringKit.getBean<DataSource>()
        ds.connection.use { conn ->
            println("conn = $conn , class = ${conn.javaClass.name}")
        }
//        assert(KudosContextHolder.currentDataSource() is DataSourceProxy)
    }

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
            // AT 模式：autoCommit=true（详见 SeataTestBase.autoCommitForMode()）
            registry.add("spring.datasource.dynamic.hikari.is-auto-commit") { "true" }
            registry.add("spring.datasource.dynamic.datasource.postgres.hikari.is-auto-commit") { "true" }
            // AT 下 Seata auto-proxy 必须开（它才是真正提供 ConnectionProxy 的那一环）。yml 默认就是
            // true，这里显式注册一遍只为和 XaSeataTest 对仗、让意图一目了然。
            registry.add("seata.enable-auto-data-source-proxy") { "true" }
            startContainer(registry)
        }
    }

}
