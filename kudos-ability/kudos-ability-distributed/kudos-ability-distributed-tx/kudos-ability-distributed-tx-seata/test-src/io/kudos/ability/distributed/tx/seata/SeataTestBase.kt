package io.kudos.ability.distributed.tx.seata

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ability.distributed.tx.seata.main.IService
import io.kudos.ability.distributed.tx.seata.ms1.Application1
import io.kudos.ability.distributed.tx.seata.ms2.Application2
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
 * Base class for Seata distributed-transaction test cases.
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

    /**
     * AT mode requires autoCommit=true (so Seata's ConnectionProxy can intercept each SQL's
     * auto-commit to write the undo log / register the branch / actually commit business data);
     * XA mode requires autoCommit=false (the XA protocol requires the whole SQL block to live
     * inside a single XA transaction; autoCommit=true would shatter XA state).
     * The `is-auto-commit` value in yml is the default; here it is explicitly overridden per
     * test mode for the sub-app via CLI args, and overridden symmetrically on the main side
     * via each test class's `@DynamicPropertySource`.
     */
    protected open fun autoCommitForMode(): Boolean = dataSourceProxyMode() == "AT"

    @BeforeAll
    fun setUp() {
        val url = "jdbc:postgresql://localhost:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val sharedArgs = buildList {
            add("--seata.service.vgroup-mapping.default_tx_group=default")
            add("--seata.service.vgroup-mapping.other_tx_group=default")
            add("--seata.tx-service-group=${txServiceGroup()}")
            add("--seata.data-source-proxy-mode=${dataSourceProxyMode()}")
            add("--spring.datasource.dynamic.datasource.postgres.url=$url")
            add("--spring.datasource.dynamic.hikari.is-auto-commit=${autoCommitForMode()}")
            add("--spring.datasource.dynamic.datasource.postgres.hikari.is-auto-commit=${autoCommitForMode()}")
            // Seata auto-proxy is the key to obtaining the ConnectionProxy interception chain in
            // AT mode; it MUST be on (true) under AT.
            // Under XA, baomidou dynamic.seata=true has already wrapped DataSourceProxyXA; letting
            // Seata wrap a second time makes ConnectionProxyXA.init <-> DataSourceProxyXA.getConnection
            // call each other into StackOverflowError, so XA MUST keep it off (false).
            add("--seata.enable-auto-data-source-proxy=${dataSourceProxyMode() != "XA"}")
            // Under XA we also tell baomidou: use the XA proxy instead of the default AT. Under AT,
            // keep the default behavior — explicitly setting AT would make baomidou take a different
            // code path that breaks AT (verified empirically).
            if (dataSourceProxyMode() == "XA") {
                add("--spring.datasource.dynamic.seata-mode=XA")
            }
        }.toTypedArray()

        SpringApplication.run(
            Application1::class.java,
            *sharedArgs,
            "--spring.application.name=${app1Name()}",
            "--server.port=${app1Port()}",
        )

        SpringApplication.run(
            Application2::class.java,
            *sharedArgs,
            "--spring.application.name=${app2Name()}",
            "--server.port=${app2Port()}",
        )
    }

    @Test
    fun globalTxId() {
        Assertions.assertNotNull(service.getGlobalTxId()) // A null global tx id is most likely an environment issue
    }

    @Test
    fun autoCommit() {
        // AT mode: autoCommit MUST be true — Seata's ConnectionProxy relies on the interception
        //          chain triggered by each SQL's auto-commit to "write the undo log + register
        //          the branch + actually commit". With autoCommit=false, each SQL enters an
        //          implicit open transaction that is never explicitly committed and gets rolled
        //          back when Hikari returns the connection to the pool, losing data.
        // XA mode: autoCommit MUST be false — the XA protocol requires the entire SQL block to
        //          live in a single XA transaction; autoCommit=true would shatter XA state.
        val expectedAutoCommit = dataSourceProxyMode() == "AT"
        RdbKit.getDatabase().useConnection { conn ->
            kotlin.test.assertEquals(
                expectedAutoCommit, conn.autoCommit,
                "${dataSourceProxyMode()} mode requires conn.autoCommit=$expectedAutoCommit"
            )
        }
    }

    /**
     * Local transaction test.
     */
    open fun localTx() {
        // Branch transaction throws -> everything rolls back
//        Assertions.assertThrows(Exception::class.java) { service.onBranchErrorLocal() }
//        assertEquals(100.0, service.getById(1).balance)
//        assertEquals(200.0, service.getById(2).balance)
//
//        // Global transaction throws -> everything rolls back
//        Assertions.assertThrows(Exception::class.java) { service.onGlobalErrorLocal() }
//        assertEquals(100.0, service.getById(1).balance)
//        assertEquals(200.0, service.getById(2).balance)

        // No exception -> all branch transactions commit
        service.normalLocal()
        assertEquals(50.0, service.getById(1).balance)
        assertEquals(250.0, service.getById(2).balance)
    }

    /**
     * Remote transaction test.
     */
    open fun remoteTx() {
        // Branch transaction throws -> everything rolls back
        Assertions.assertThrows(Exception::class.java) { service.onBranchErrorRemote() }
        assertEquals(100.0, service.getById(1).balance)
        assertEquals(200.0, service.getById(2).balance)

        // Global transaction throws -> everything rolls back
        Assertions.assertThrows(Exception::class.java) { service.onGlobalErrorRemote() }
        assertEquals(100.0, service.getById(1).balance)
        assertEquals(200.0, service.getById(2).balance)

        // No exception -> all branch transactions commit
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
