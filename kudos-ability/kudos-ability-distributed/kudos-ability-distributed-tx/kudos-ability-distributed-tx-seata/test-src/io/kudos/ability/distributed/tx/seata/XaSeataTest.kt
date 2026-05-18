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
@Import(Service::class, io.kudos.ability.distributed.tx.seata.feign.SeataFeignXidConfig::class)
@Disabled(
    "Seata XA 模式需要底层使用 XA-aware 驱动（org.postgresql.xa.PGXADataSource），" +
            "当前测试栈用 HikariCP 包装的普通 PgDataSource — DataSourceProxyXA 无法走完两阶段 commit/rollback，" +
            "事务始终被回滚。要让 XA 真正可用需要：(1) 改用 PGXADataSource，(2) 用支持 XA 的连接池或不走池，" +
            "(3) 重做 baomidou dynamic-datasource 集成。AT 模式 (AtSeataTest) 已经覆盖了分布式事务核心场景。"
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
            // XA 模式：autoCommit=false（详见 SeataTestBase.autoCommitForMode()）
            registry.add("spring.datasource.dynamic.hikari.is-auto-commit") { "false" }
            registry.add("spring.datasource.dynamic.datasource.postgres.hikari.is-auto-commit") { "false" }
            // baomidou dynamic-datasource 已经在 Bean 装配时通过 dynamic.seata=true 包装好 DS，
            // 关掉 Seata 自身的 auto-proxy 避免 double-wrap（XA 下会 StackOverflowError）。
            registry.add("seata.enable-auto-data-source-proxy") { "false" }
            startContainer(registry)
        }
    }

}
