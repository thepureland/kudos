package io.kudos.ability.distributed.stream.rocketmq.support

import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.rocketmq.init.properties.RocketMqProperties
import io.kudos.context.kit.SpringKit
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer
import org.apache.rocketmq.client.exception.MQClientException
import org.apache.rocketmq.common.message.MessageExt
import org.mockito.ArgumentCaptor
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.InvalidClassException
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [RocketMqBatchConsumer] unit tests (no real RocketMQ; [DefaultLitePullConsumer] is replaced via
 * Mockito constructor mocking). Covers:
 * - constructor wiring: namesrvAddr / pullBatchSize / pull timeout / autoCommit / subscribe / start
 * - constructor MQClientException branch: subscribe failure is caught and logged, no rethrow
 * - daemon loop: batch-size trigger, pull-time trigger, idle sleep, InterruptedException break,
 *   final flush on exit
 * - toProcessBizData: empty batch early-return, null-processor warn branch, deserialization failure
 *   wrapped in RuntimeException, success commit, failure with saveException=false (no commit),
 *   failure with saveException=true (persist + commit), persistence disabled by properties (warn,
 *   commit, no save), null-topic requireNotNull message
 * - default constructor args resolved from Spring ([RocketMqProperties.instance] and
 *   `SpringKit.getBean<ISysMqFailMsgService>()`)
 * - destroy: null daemon thread, shutdown exception warn branch, InterruptedException on join
 * - decodeJdkBody: filter allow / reject and blank-filter (unrestricted) paths
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RocketMqBatchConsumerTest {

    // ---------------------------------------------------------------------
    // decodeJdkBody (pure static logic)
    // ---------------------------------------------------------------------

    @Test
    fun decodeJdkBody_allowsClassMatchedByFilter() {
        val payload = TestPayload("ok")

        val decoded = RocketMqBatchConsumer.decodeJdkBody(
            serialize(payload),
            "io.kudos.ability.distributed.stream.rocketmq.support.*;java.base/*;!*"
        )

        assertEquals(payload, decoded)
    }

    @Test
    fun decodeJdkBody_rejectsClassNotMatchedByFilter() {
        val payload = TestPayload("blocked")

        assertFailsWith<InvalidClassException> {
            RocketMqBatchConsumer.decodeJdkBody(serialize(payload), "java.base/*;!*")
        }
    }

    @Test
    fun decodeJdkBody_blankFilterIsUnrestricted() {
        val payload = TestPayload("unicode-值-é")

        assertEquals(payload, RocketMqBatchConsumer.decodeJdkBody(serialize(payload), ""))
        assertEquals(payload, RocketMqBatchConsumer.decodeJdkBody(serialize(payload), "   "))
    }

    // ---------------------------------------------------------------------
    // Constructor wiring
    // ---------------------------------------------------------------------

    @Test
    fun constructor_configuresAndStartsLitePullConsumer() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer(
                groupName = "g-ctor", topic = "t-ctor", batchProcessSize = 10, pullTime = 60_000,
                props = props(nameSrv = "10.1.2.3:9876")
            )
            try {
                val mock = construction.constructed().single()
                Mockito.verify(mock).namesrvAddr = "10.1.2.3:9876"
                Mockito.verify(mock).pullBatchSize = 32
                Mockito.verify(mock).consumerPullTimeoutMillis = 5000L
                Mockito.verify(mock).isAutoCommit = false
                Mockito.verify(mock).subscribe("t-ctor", "*")
                Mockito.verify(mock).start()
            } finally {
                consumer.destroy()
            }
        }
    }

    @Test
    fun constructor_subscribeFailureIsCaughtNotRethrown() {
        Mockito.mockConstruction(DefaultLitePullConsumer::class.java) { mock, _ ->
            Mockito.doThrow(MQClientException("subscribe boom", null))
                .`when`(mock).subscribe(Mockito.anyString(), Mockito.anyString())
        }.use { construction ->
            // Must not throw even though subscribe failed inside init
            val consumer = newConsumer("g-fail", "t-fail", 10, 60_000, props())
            val mock = construction.constructed().single()
            Mockito.verify(mock, Mockito.never()).start()
            consumer.destroy()
        }
    }

    // ---------------------------------------------------------------------
    // Daemon polling loop
    // ---------------------------------------------------------------------

    @Test
    fun start_batchSizeTriggerProcessesAndCommits() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-batch", "t-batch", 2, 60_000, props())
            try {
                val mock = construction.constructed().single()
                val msg1 = messageOf(TestPayload("a"), "traceId" to "t-1")
                val msg2 = messageOf(TestPayload("b"))
                Mockito.`when`(mock.poll()).thenReturn(listOf(msg1, msg2), emptyList())

                val received = AtomicReference<List<RocketMqBatchConsumer.BatchConsumerItem<TestPayload>>>()
                val latch = CountDownLatch(1)
                consumer.start { items ->
                    received.set(items)
                    latch.countDown()
                }

                assertTrue(latch.await(10, TimeUnit.SECONDS), "batch was not processed in time")
                Mockito.verify(mock, Mockito.timeout(10_000)).commit()

                val items = received.get()
                assertEquals(2, items.size)
                assertEquals(TestPayload("a"), items[0].data)
                assertEquals("t-1", items[0].properties?.get("traceId"))
                assertEquals(TestPayload("b"), items[1].data)
            } finally {
                consumer.destroy()
            }
        }
    }

    @Test
    fun start_pullTimeTriggerProcessesSmallBatch() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-time", "t-time", 100, 50, props())
            try {
                val mock = construction.constructed().single()
                val msg = messageOf(TestPayload("only-one"))
                Mockito.`when`(mock.poll()).thenReturn(listOf(msg), emptyList())

                val received = AtomicReference<List<RocketMqBatchConsumer.BatchConsumerItem<TestPayload>>>()
                val latch = CountDownLatch(1)
                consumer.start { items ->
                    received.set(items)
                    latch.countDown()
                }

                assertTrue(latch.await(10, TimeUnit.SECONDS), "time-triggered batch was not processed")
                Mockito.verify(mock, Mockito.timeout(10_000)).commit()
                assertEquals(1, received.get().size)
                assertEquals(TestPayload("only-one"), received.get()[0].data)
            } finally {
                consumer.destroy()
            }
        }
    }

    @Test
    fun start_interruptDuringIdleSleepStopsThread() {
        withMockedLitePullConsumer { _ ->
            val consumer = newConsumer("g-interrupt", "t-interrupt", 1000, 60_000, props())
            val invoked = AtomicBoolean(false)
            consumer.start { invoked.set(true) }

            val daemon = awaitDaemonThread("RocketMqBatchConsumer-g-interrupt")
            Thread.sleep(150) // let the loop reach its idle sleep
            daemon.interrupt()
            daemon.join(10_000)

            assertFalse(daemon.isAlive, "daemon thread should exit after interrupt")
            assertFalse(invoked.get(), "processor must not run for an empty final batch")
            consumer.destroy()
        }
    }

    // ---------------------------------------------------------------------
    // toProcessBizData branches (direct invocation; daemon kept idle)
    // ---------------------------------------------------------------------

    @Test
    fun toProcessBizData_emptyBatchReturnsWithoutTouchingConsumer() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-empty", "t-empty", 10, 60_000, props())
            val mock = construction.constructed().single()

            consumer.toProcessBizData(mutableListOf())
            consumer.toProcessBizData(mutableListOf(null)) // null entries are filtered, list becomes empty -> null-processor warn

            Mockito.verify(mock, Mockito.never()).commit()
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_nullProcessorSkipsBatchWithoutCommit() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-noproc", "t-noproc", 10, 60_000, props())
            val mock = construction.constructed().single()

            // start() never called -> bizBatchProcess is null
            consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("x"))))

            Mockito.verify(mock, Mockito.never()).commit()
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_corruptBodyThrowsRuntimeException() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-corrupt", "t-corrupt", 10, 60_000, props())
            val mock = construction.constructed().single()
            val badMsg = MessageExt().apply { body = "not-a-jdk-serialized-stream".toByteArray() }

            val e = assertFailsWith<RuntimeException> {
                consumer.toProcessBizData(mutableListOf(badMsg))
            }
            assertNotNull(e.cause, "original deserialization exception must be preserved as cause")
            Mockito.verify(mock, Mockito.never()).commit()
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_successCommitsOffset() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-ok", "t-ok", 1000, 60_000, props())
            val mock = construction.constructed().single()
            val received = AtomicReference<List<RocketMqBatchConsumer.BatchConsumerItem<TestPayload>>>()
            consumer.start { received.set(it) } // daemon stays idle: poll()=null, pullTime huge

            consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("direct"))))

            assertEquals(TestPayload("direct"), received.get().single().data)
            Mockito.verify(mock).commit()
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_failureWithoutSaveExceptionDoesNotCommit() {
        withMockedLitePullConsumer { construction ->
            val failMsgService = Mockito.mock(ISysMqFailMsgService::class.java)
            val consumer = newConsumer(
                "g-nosave", "t-nosave", 1000, 60_000, props(),
                saveException = false, failMsgService = failMsgService
            )
            val mock = construction.constructed().single()
            consumer.start { error("biz boom") }

            consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("x"))))

            Mockito.verify(mock, Mockito.never()).commit()
            Mockito.verify(failMsgService, Mockito.never()).save(Mockito.any() ?: SysMqFailMsg())
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_failureWithSaveExceptionPersistsAndCommits() {
        withMockedLitePullConsumer { construction ->
            val failMsgService = Mockito.mock(ISysMqFailMsgService::class.java)
            val consumer = newConsumer(
                "g-save", "t-save", 1000, 60_000, props(saveException = true),
                saveException = true, failMsgService = failMsgService
            )
            val mock = construction.constructed().single()
            consumer.start { error("biz boom") }

            consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("x"))))

            val captor = ArgumentCaptor.forClass(SysMqFailMsg::class.java)
            Mockito.verify(failMsgService).save(captor.capture() ?: SysMqFailMsg())
            assertEquals("t-save", captor.value.topic)
            assertTrue(captor.value.msgBodyJson.isNotBlank(), "failed batch must be serialized to JSON")
            assertNotNull(captor.value.createTime)
            Mockito.verify(mock).commit() // committed even on failure because the data was persisted
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_persistenceDisabledByPropertiesStillCommitsButDoesNotSave() {
        withMockedLitePullConsumer { construction ->
            val failMsgService = Mockito.mock(ISysMqFailMsgService::class.java)
            val consumer = newConsumer(
                "g-propoff", "t-propoff", 1000, 60_000, props(saveException = false),
                saveException = true, failMsgService = failMsgService
            )
            val mock = construction.constructed().single()
            consumer.start { error("biz boom") }

            consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("x"))))

            Mockito.verify(failMsgService, Mockito.never()).save(Mockito.any() ?: SysMqFailMsg())
            Mockito.verify(mock).commit()
            consumer.destroy()
        }
    }

    @Test
    fun toProcessBizData_nullTopicFailsRequireNotNullWhenPersisting() {
        withMockedLitePullConsumer { _ ->
            val failMsgService = Mockito.mock(ISysMqFailMsgService::class.java)
            val consumer = newConsumer(
                "g-nulltopic", null, 1000, 60_000, props(saveException = true),
                saveException = true, failMsgService = failMsgService
            )
            consumer.start { error("biz boom") }

            val e = assertFailsWith<IllegalArgumentException> {
                consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("x"))))
            }
            assertEquals("topic is null", e.message)
            consumer.destroy()
        }
    }

    // ---------------------------------------------------------------------
    // Default constructor arguments resolved from Spring
    // ---------------------------------------------------------------------

    @Test
    fun defaultArguments_resolveRocketMqPropertiesAndFailMsgServiceFromSpring() {
        val springProps = props(nameSrv = "spring-resolved:9876", saveException = true)
        val failMsgService = Mockito.mock(ISysMqFailMsgService::class.java)
        val ctx = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(ctx.getBean(RocketMqProperties::class.java)).thenReturn(springProps)
        Mockito.`when`(ctx.getBean(ISysMqFailMsgService::class.java)).thenReturn(failMsgService)

        val previousCtx = try {
            SpringKit.applicationContext
        } catch (_: IllegalStateException) {
            null
        }
        SpringKit.applicationContext = ctx
        try {
            withMockedLitePullConsumer { construction ->
                val consumer = RocketMqBatchConsumer<TestPayload>("g-spring", "t-spring", 1000, 60_000, true)
                val mock = construction.constructed().single()
                Mockito.verify(mock).namesrvAddr = "spring-resolved:9876"

                consumer.start { error("biz boom") }
                consumer.toProcessBizData(mutableListOf(messageOf(TestPayload("x"))))

                Mockito.verify(failMsgService).save(Mockito.any() ?: SysMqFailMsg())
                Mockito.verify(mock).commit()
                consumer.destroy()
            }
        } finally {
            SpringKit.applicationContext = previousCtx
        }
    }

    // ---------------------------------------------------------------------
    // destroy
    // ---------------------------------------------------------------------

    @Test
    fun destroy_withoutStartShutsDownConsumer() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-destroy", "t-destroy", 10, 60_000, props())
            val mock = construction.constructed().single()

            consumer.destroy() // daemonThread is null -> join skipped

            Mockito.verify(mock).shutdown()
        }
    }

    @Test
    fun destroy_swallowsShutdownException() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-shutboom", "t-shutboom", 10, 60_000, props())
            val mock = construction.constructed().single()
            Mockito.doThrow(RuntimeException("shutdown boom")).`when`(mock).shutdown()

            consumer.destroy() // must not propagate

            Mockito.verify(mock).shutdown()
        }
    }

    @Test
    fun destroy_interruptedWhileJoiningKeepsInterruptFlag() {
        withMockedLitePullConsumer { construction ->
            val consumer = newConsumer("g-joinint", "t-joinint", 1000, 60_000, props())
            val mock = construction.constructed().single()
            // Make the daemon thread slow to exit so join() blocks long enough to be interrupted
            Mockito.`when`(mock.poll()).thenAnswer {
                Thread.sleep(2000)
                emptyList<MessageExt>()
            }
            consumer.start { }
            awaitDaemonThread("RocketMqBatchConsumer-g-joinint")

            val interruptedAfterDestroy = AtomicBoolean(false)
            val destroyed = CountDownLatch(1)
            val helper = Thread {
                consumer.destroy()
                interruptedAfterDestroy.set(Thread.currentThread().isInterrupted)
                destroyed.countDown()
            }
            helper.start()
            Thread.sleep(200) // let helper enter join()
            helper.interrupt()

            assertTrue(destroyed.await(10, TimeUnit.SECONDS), "destroy() must return after interrupt")
            assertTrue(interruptedAfterDestroy.get(), "destroy() must re-set the interrupt flag")
            Mockito.verify(mock, Mockito.timeout(10_000)).shutdown()
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun withMockedLitePullConsumer(block: (MockedConstruction<DefaultLitePullConsumer>) -> Unit) {
        Mockito.mockConstruction(DefaultLitePullConsumer::class.java).use(block)
    }

    private fun props(
        nameSrv: String? = "127.0.0.1:9876",
        saveException: Boolean = true,
        filter: String = ""
    ) = RocketMqProperties().apply {
        nameSrvAddr = nameSrv
        this.saveException = saveException
        batchConsumerDeserializationFilter = filter
    }

    private fun newConsumer(
        groupName: String?,
        topic: String?,
        batchProcessSize: Int,
        pullTime: Long,
        props: RocketMqProperties,
        saveException: Boolean = false,
        failMsgService: ISysMqFailMsgService = Mockito.mock(ISysMqFailMsgService::class.java)
    ): RocketMqBatchConsumer<TestPayload> = RocketMqBatchConsumer(
        groupName, topic, batchProcessSize, pullTime, saveException, props
    ) { failMsgService }

    private fun messageOf(payload: TestPayload, vararg userProps: Pair<String, String>): MessageExt =
        MessageExt().apply {
            body = serialize(payload)
            // fastjson2 walks every getter when persisting a failed batch; MessageExt's
            // getBornHostString()/getStoreHost() NPE on null addresses, so populate them.
            bornHost = java.net.InetSocketAddress("127.0.0.1", 10911)
            storeHost = java.net.InetSocketAddress("127.0.0.1", 10911)
            userProps.forEach { (k, v) -> putUserProperty(k, v) }
        }

    private fun awaitDaemonThread(name: String): Thread {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            Thread.getAllStackTraces().keys.firstOrNull { it.name == name }?.let { return it }
            Thread.sleep(20)
        }
        error("daemon thread '$name' did not start in time")
    }

    private fun serialize(value: Serializable): ByteArray {
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use { it.writeObject(value) }
        return output.toByteArray()
    }

    /**
     * Test DTO for RocketMQ batch consumer deserialization.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    internal data class TestPayload(val value: String) : Serializable
}
