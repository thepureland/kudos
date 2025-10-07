package io.kudos.ability.distributed.tx.seata

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ability.distributed.tx.seata.main.IService
import io.kudos.ability.distributed.tx.seata.ms1.Application1
import io.kudos.ability.distributed.tx.seata.ms2.Application2
import io.kudos.base.net.IpKit
import io.kudos.test.container.containers.PostgresTestContainer
import io.kudos.test.container.containers.SeataTestContainer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.jdbc.Sql
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * seata分布式事务测试用例基类
 *
 * @author K
 * @since 1.0.0
 */
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("main")
@Sql(
    scripts = ["/sql/postgres/reset.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
abstract class SeataTestBase {
    
    @Autowired
    private lateinit var service: IService

    @BeforeAll
    fun setUp() {
        val url = "jdbc:postgresql://localhost:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val args1 = arrayOf(
            "--seata.service.vgroup-mapping.default_tx_group=default",
            "--seata.service.vgroup-mapping.other_tx_group=default",
            "--seata.tx-service-group=${txServiceGroup()}",
            "--seata.data-source-proxy-mode=${dataSourceProxyMode()}",
            "--spring.datasource.dynamic.datasource.postgres.url=$url",
            "--spring.application.name=${app1Name()}",
            "--server.port=${app1Port()}"
        )
        SpringApplication.run(Application1::class.java, *args1)

        val args2 = arrayOf(
            "--seata.service.vgroup-mapping.default_tx_group=default",
            "--seata.service.vgroup-mapping.other_tx_group=default",
            "--seata.tx-service-group=${txServiceGroup()}",
            "--seata.data-source-proxy-mode=${dataSourceProxyMode()}",
            "--spring.datasource.dynamic.datasource.postgres.url=$url",
            "--spring.application.name=${app2Name()}",
            "--server.port=${app2Port()}"
        )
        SpringApplication.run(Application2::class.java, *args2)
    }

    @Test
    fun globalTxId() {
        Assertions.assertNotNull(service.getGlobalTxId()) // 全局事务id为null很有可能是环境问题
    }

    @Test
    fun autoCommit() {
        RdbKit.getDatabase().useConnection { conn ->
            assertFalse(conn.autoCommit)
        }
    }

    /**
     * 本地事务测试
     */
    open fun localTx() {
        // 分支事务异常，全部回滚
//        Assertions.assertThrows(Exception::class.java) { service.onBranchErrorLocal() }
//        assertEquals(100.0, service.getById(1).balance)
//        assertEquals(200.0, service.getById(2).balance)
//
//        // 全局事务异常，全部回滚
//        Assertions.assertThrows(Exception::class.java) { service.onGlobalErrorLocal() }
//        assertEquals(100.0, service.getById(1).balance)
//        assertEquals(200.0, service.getById(2).balance)

        // 无异常，分支事务全部提交
        service.normalLocal()
        assertEquals(50.0, service.getById(1).balance)
        assertEquals(250.0, service.getById(2).balance)
    }

    /**
     * 远程事务测试
     */
    open fun remoteTx() {
        // 分支事务异常，全部回滚
        Assertions.assertThrows(Exception::class.java) { service.onBranchErrorRemote() }
        assertEquals(100.0, service.getById(1).balance)
        assertEquals(200.0, service.getById(2).balance)

        // 全局事务异常，全部回滚
        Assertions.assertThrows(Exception::class.java) { service.onGlobalErrorRemote() }
        assertEquals(100.0, service.getById(1).balance)
        assertEquals(200.0, service.getById(2).balance)

        // 无异常，分支事务全部提交
        service.normalRemote()
        assertEquals(50.0, service.getById(1).balance)
        assertEquals(250.0, service.getById(2).balance)
    }

    protected abstract fun txServiceGroup(): String

    protected abstract fun dataSourceProxyMode(): String

    protected abstract fun app1Port(): Int

    protected abstract fun app2Port(): Int

    protected abstract fun app1Name(): String

    protected abstract fun app2Name(): String

    companion object {
        @JvmStatic
        protected fun startContainer(registry: DynamicPropertyRegistry) {
            val postgresThread = Thread { PostgresTestContainer.startIfNeeded(registry) }
            val seataThread = Thread { SeataTestContainer.startIfNeeded(registry) }

            postgresThread.start()
            seataThread.start()

            postgresThread.join()
            seataThread.join()
        }
    }

}
