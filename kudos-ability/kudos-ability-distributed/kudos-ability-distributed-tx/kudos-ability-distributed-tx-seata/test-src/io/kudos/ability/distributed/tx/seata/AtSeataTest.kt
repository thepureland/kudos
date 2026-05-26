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
 * Seata AT mode test case.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(Service::class)
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
            // AT mode: autoCommit=true (see SeataTestBase.autoCommitForMode() for details)
            registry.add("spring.datasource.dynamic.hikari.is-auto-commit") { "true" }
            registry.add("spring.datasource.dynamic.datasource.postgres.hikari.is-auto-commit") { "true" }
            // Under AT, Seata auto-proxy must be enabled (it is what actually provides ConnectionProxy).
            // The yml default is already true; registering it explicitly here only mirrors XaSeataTest so
            // the intent is obvious at a glance.
            registry.add("seata.enable-auto-data-source-proxy") { "true" }
            startContainer(registry)
        }
    }

}
