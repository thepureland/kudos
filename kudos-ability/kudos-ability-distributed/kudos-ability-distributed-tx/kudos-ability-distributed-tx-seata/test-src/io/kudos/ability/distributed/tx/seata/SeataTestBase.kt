package io.kudos.ability.distributed.tx.seata

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import org.soul.ability.distributed.tx.seata.data.TestTableKit
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * seata分布式事务测试用例基类
 *
 * @author will
 * @since 5.1.1
 */
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("main")
abstract class SeataTestBase {
    @Autowired
    private val service: IService? = null

    @BeforeEach
    fun beforeEach() {
        TestTableKit.delete()
        TestTableKit.insert()
    }

    @BeforeAll
    fun setUp() {
        TestTableKit.create()

        val url: String = "jdbc:postgresql://%s:%s/%s".formatted(
            IpTool.getLocalIp(),
            TestContainerPostgres.PORT,
            TestContainerPostgres.DATABASE
        )
        val args1 = arrayOf<String?>(
            "--seata.service.vgroup-mapping.default_tx_group=default",
            "--seata.service.vgroup-mapping.other_tx_group=default",
            "--seata.tx-service-group=" + this.txServiceGroup,
            "--seata.data-source-proxy-mode=" + this.dataSourceProxyMode,
            "--spring.datasource.dynamic.datasource.postgres.url=" + url,
            "--spring.application.name=" + this.app1Name,
            "--server.port=" + this.app1Port
        )
        SpringApplication.run(Application1::class.java, *args1)

        val args2 = arrayOf<String?>(
            "--seata.service.vgroup-mapping.default_tx_group=default",
            "--seata.service.vgroup-mapping.other_tx_group=default",
            "--seata.tx-service-group=" + this.txServiceGroup,
            "--seata.data-source-proxy-mode=" + this.dataSourceProxyMode,
            "--spring.datasource.dynamic.datasource.postgres.url=" + url,
            "--spring.application.name=" + this.app2Name,
            "--server.port=" + this.app2Port
        )
        SpringApplication.run(Application2::class.java, *args2)
    }

    @Test
    fun globalTxId() {
        Assertions.assertNotNull(service.getGlobalTxId()) // 全局事务id为null很有可能是环境问题
    }

    /**
     * 本地事务测试
     */
    open fun localTx() {
        // 分支事务异常，全部回滚
        Assertions.assertThrows<Exception?>(Exception::class.java, Executable { service.onBranchErrorLocal() })
        assertEquals(100.0, service.getById(1).getBalance())
        assertEquals(200.0, service.getById(2).getBalance())

        // 全局事务异常，全部回滚
        Assertions.assertThrows<Exception?>(Exception::class.java, Executable { service.onGlobalErrorLocal() })
        assertEquals(100.0, service.getById(1).getBalance())
        assertEquals(200.0, service.getById(2).getBalance())

        // 无异常，分支事务全部提交
        service.normalLocal()
        assertEquals(50.0, service.getById(1).getBalance())
        assertEquals(250.0, service.getById(2).getBalance())
    }

    /**
     * 远程事务测试
     */
    open fun remoteTx() {
        // 分支事务异常，全部回滚
        Assertions.assertThrows<Exception?>(Exception::class.java, Executable { service.onBranchErrorRemote() })
        assertEquals(100.0, service.getById(1).getBalance())
        assertEquals(200.0, service.getById(2).getBalance())

        // 全局事务异常，全部回滚
        Assertions.assertThrows<Exception?>(Exception::class.java, Executable { service.onGlobalErrorRemote() })
        assertEquals(100.0, service.getById(1).getBalance())
        assertEquals(200.0, service.getById(2).getBalance())

        // 无异常，分支事务全部提交
        service.normalRemote()
        assertEquals(50.0, service.getById(1).getBalance())
        assertEquals(250.0, service.getById(2).getBalance())
    }

    protected abstract val txServiceGroup: String?

    protected abstract val dataSourceProxyMode: String?

    protected abstract val app1Port: Int

    protected abstract val app2Port: Int

    protected abstract val app1Name: String?

    protected abstract val app2Name: String?

    companion object {
        protected fun registerProperties(registry: DynamicPropertyRegistry?) {
            TestContainerPostgres.start(registry)
            TestContainerSeata.start(registry)
        }
    }
}
