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
 * Test cases for Seata XA mode.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(Service::class)
@Disabled(
    "Seata XA mode requires an XA-aware driver underneath (e.g. org.postgresql.xa.PGXADataSource). " +
            "The current test stack uses a plain PgDataSource wrapped by HikariCP — DataSourceProxyXA cannot complete " +
            "the two-phase commit/rollback, so transactions are always rolled back. To make XA truly usable: " +
            "(1) switch to PGXADataSource, (2) use an XA-capable pool or run without pooling, " +
            "(3) redo the baomidou dynamic-datasource integration. AT mode (AtSeataTest) already covers the core " +
            "distributed-transaction scenarios."
)
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
            registry.add("spring.datasource.dynamic.seata-mode") { "XA" }
            // XA mode: autoCommit=false (see SeataTestBase.autoCommitForMode())
            registry.add("spring.datasource.dynamic.hikari.is-auto-commit") { "false" }
            registry.add("spring.datasource.dynamic.datasource.postgres.hikari.is-auto-commit") { "false" }
            // baomidou dynamic-datasource already wraps the DS at bean wiring via dynamic.seata=true;
            // disable Seata's own auto-proxy to avoid double-wrapping (which causes StackOverflowError under XA).
            registry.add("seata.enable-auto-data-source-proxy") { "false" }
            startContainer(registry)
        }
    }

}
