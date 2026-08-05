package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerRegistryTestOps
import io.kudos.ability.distributed.stream.common.init.properties.StreamProducerLimitProperties
import io.kudos.ability.distributed.stream.common.model.vo.StreamHeader
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.cloud.stream.config.BindingProperties
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.support.ErrorMessage
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.util.MimeType
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [StreamProducerHelper]:
 * - [StreamProducerHelper.sendMessage]: missing-binding short-circuit, message wrapping
 *   (StreamMessageVo payload + StreamHeader/SCST_BIND_NAME headers), bridge false result,
 *   bridge exception with/without registered fail handler.
 * - [StreamProducerHelper.asyncSendMessage]: missing-binding short-circuit, async success and
 *   failure logging, permit release and rethrow when the executor rejects the task.
 * - [StreamProducerHelper.doRealSend]: success, failure-to-error-channel routing, resend flag
 *   suppressing the error channel, no handler registered.
 * - producer local rate limiting: disabled/null properties, tryAcquire with and without timeout,
 *   exhausted permits, interrupted acquisition restoring the interrupt flag, limiter recreation
 *   when maxInFlight changes and coercion of non-positive maxInFlight to 1.
 *
 * Collaborators are injected via reflection; the global fail-handler registry and the Kudos
 * context are snapshotted/cleared and restored around each test.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamProducerHelperTest {

    private lateinit var snapshot: Map<String, IStreamFailHandler>
    private lateinit var bridge: StreamBridge
    private lateinit var channel: RecordingChannel

    @BeforeTest
    fun setUp() {
        snapshot = StreamFailHandlerRegistryTestOps.snapshot()
        StreamFailHandlerRegistryTestOps.clear()
        KudosContextHolder.clear()
    }

    @AfterTest
    fun tearDown() {
        StreamFailHandlerRegistryTestOps.restore(snapshot)
        KudosContextHolder.clear()
        // Defensive: never leak an interrupt flag to other tests.
        Thread.interrupted()
    }

    // ----- sendMessage -----

    @Test
    fun sendMessage_returnsFalseWhenBindingIsNotConfigured() {
        val helper = newHelper(bindings = emptyMap())

        assertFalse(helper.sendMessage("unknown-out-0", "data"))

        verifyNoInteractions(bridge)
    }

    @Test
    fun sendMessage_wrapsPayloadAndContextHeaders() {
        KudosContextHolder.set(KudosContext().apply {
            tenantId = "tenant-1"
            _datasourceTenantId = "ds-tenant-1"
            user = object : IIdEntity<String> {
                override val id = "user-1"
            }
        })
        val helper = newHelper()
        stubBridge(true)

        assertTrue(helper.sendMessage("bind-out-0", "payload-1"))

        val message = capturedMessage("bind-out-0")
        val body = assertIs<StreamMessageVo<*>>(message.payload)
        assertEquals("payload-1", body.data)
        assertEquals("bind-out-0", message.headers[StreamHeader.SCST_BIND_NAME])
        assertEquals("topic-a", message.headers[StreamHeader.TOPIC_KEY])
        assertEquals("tenant-1", message.headers[StreamHeader.TENANT_ID_KEY])
        assertEquals("ds-tenant-1", message.headers[StreamHeader.DATASOURCE_TENANT_ID])
        assertEquals("user-1", message.headers[StreamHeader.USER_ID_KEY])
    }

    @Test
    fun sendMessage_returnsFalseWhenBridgeReportsFalse() {
        val helper = newHelper()
        stubBridge(false)

        assertFalse(helper.sendMessage("bind-out-0", "data"))

        assertTrue(channel.messages.isEmpty(), "bridge returning false must not push to the error channel")
    }

    @Test
    fun sendMessage_pushesErrorMessageToChannelWhenBridgeThrowsAndHandlerRegistered() {
        val helper = newHelper()
        stubBridgeThrows(IllegalStateException("broker down"))
        StreamFailHandlerItem.put("bind-out-0", TestFailHandler("bind-out-0"))

        assertFalse(helper.sendMessage("bind-out-0", "data"))

        val error = assertIs<ErrorMessage>(channel.messages.single())
        val handling = assertIs<MessageHandlingException>(error.payload)
        val failed = assertIs<StreamMessageVo<*>>(handling.failedMessage!!.payload)
        assertEquals("data", failed.data)
    }

    @Test
    fun sendMessage_doesNotPushToChannelWhenNoHandlerRegistered() {
        val helper = newHelper()
        stubBridgeThrows(IllegalStateException("broker down"))

        assertFalse(helper.sendMessage("bind-out-0", "data"))

        assertTrue(channel.messages.isEmpty())
    }

    // ----- doRealSend -----

    @Test
    fun doRealSend_returnsTrueOnBridgeSuccess() {
        val helper = newHelper()
        stubBridge(true)
        val msg = org.springframework.messaging.support.GenericMessage(StreamMessageVo<Any?>("x"))

        assertTrue(helper.doRealSend("bind-out-0", msg, false))
    }

    @Test
    fun doRealSend_doesNotPushToChannelOnResendFailure() {
        val helper = newHelper()
        stubBridgeThrows(IllegalStateException("still down"))
        StreamFailHandlerItem.put("bind-out-0", TestFailHandler("bind-out-0"))
        val msg = org.springframework.messaging.support.GenericMessage(StreamMessageVo<Any?>("x"))

        assertFalse(helper.doRealSend("bind-out-0", msg, true))

        assertTrue(channel.messages.isEmpty(), "resend failures must not re-trigger failure handling")
    }

    // ----- asyncSendMessage -----

    @Test
    fun asyncSendMessage_skipsWhenBindingIsNotConfigured() {
        val helper = newHelper(bindings = emptyMap())

        helper.asyncSendMessage("unknown-out-0", "data")

        verifyNoInteractions(bridge)
    }

    @Test
    fun asyncSendMessage_sendsViaExecutor() {
        val helper = newHelper()
        stubBridge(true)

        helper.asyncSendMessage("bind-out-0", "async-payload")

        val message = capturedMessage("bind-out-0")
        assertEquals("async-payload", assertIs<StreamMessageVo<*>>(message.payload).data)
    }

    @Test
    fun asyncSendMessage_logsWarningWhenSendFails() {
        val helper = newHelper()
        stubBridge(false)

        // Must not throw even though the bridge reports failure.
        helper.asyncSendMessage("bind-out-0", "async-payload")

        capturedMessage("bind-out-0")
    }

    @Test
    fun asyncSendMessage_releasesPermitAndRethrowsWhenExecutorRejects() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 1
        }
        val helper = newHelper(executor = RejectingExecutor(), limit = limit)

        assertFailsWith<RejectedExecutionException> {
            helper.asyncSendMessage("bind-out-0", "data")
        }

        assertEquals(1, limiterOf(helper).availablePermits(), "permit must be released after rejection")
    }

    @Test
    fun asyncSendMessage_skipsWhenPermitsExhausted() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 1
            acquireTimeoutMillis = 0
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)
        assertTrue(helper.sendMessage("bind-out-0", "warm-up"))
        limiterOf(helper).acquire()

        try {
            helper.asyncSendMessage("bind-out-0", "rejected")
        } finally {
            limiterOf(helper).release()
        }

        // Only the warm-up send must have reached the bridge.
        verify(bridge).send(eq("bind-out-0"), any(Any::class.java), eq(StreamMessageConverter.MESSAGE_TYPE))
    }

    @Test
    fun asyncSendMessage_releasesPermitAfterAsyncCompletion() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 2
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)

        helper.asyncSendMessage("bind-out-0", "data")

        assertEquals(2, limiterOf(helper).availablePermits())
    }

    // ----- producer rate limiting -----

    @Test
    fun sendMessage_passesThroughWhenLimitPropertiesDisabled() {
        val limit = StreamProducerLimitProperties().apply { enabled = false }
        val helper = newHelper(limit = limit)
        stubBridge(true)

        assertTrue(helper.sendMessage("bind-out-0", "data"))
    }

    @Test
    fun sendMessage_releasesPermitAfterSendWhenLimitEnabled() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 3
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)

        assertTrue(helper.sendMessage("bind-out-0", "data"))

        assertEquals(3, limiterOf(helper).availablePermits())
    }

    @Test
    fun sendMessage_returnsFalseWhenPermitsExhaustedWithoutTimeout() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 1
            acquireTimeoutMillis = 0
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)
        assertTrue(helper.sendMessage("bind-out-0", "warm-up"))
        limiterOf(helper).acquire()

        try {
            assertFalse(helper.sendMessage("bind-out-0", "rejected"))
        } finally {
            limiterOf(helper).release()
        }
    }

    @Test
    fun sendMessage_returnsFalseWhenPermitsExhaustedWithTimeout() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 1
            acquireTimeoutMillis = 10
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)
        assertTrue(helper.sendMessage("bind-out-0", "warm-up"))
        limiterOf(helper).acquire()

        try {
            assertFalse(helper.sendMessage("bind-out-0", "rejected"))
        } finally {
            limiterOf(helper).release()
        }
    }

    @Test
    fun sendMessage_returnsFalseAndRestoresInterruptFlagWhenAcquireInterrupted() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 1
            acquireTimeoutMillis = 50
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)

        Thread.currentThread().interrupt()
        val result = helper.sendMessage("bind-out-0", "data")
        val interrupted = Thread.interrupted() // also clears the flag

        assertFalse(result)
        assertTrue(interrupted, "the interrupt flag must be restored after InterruptedException")
        verifyNoInteractions(bridge)
    }

    @Test
    fun producerLimiter_isRecreatedWhenMaxInFlightChanges() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 2
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)
        assertTrue(helper.sendMessage("bind-out-0", "a"))
        val first = limiterOf(helper)

        limit.maxInFlight = 3
        assertTrue(helper.sendMessage("bind-out-0", "b"))
        val second = limiterOf(helper)

        assertNotSame(first, second)
        assertEquals(3, second.availablePermits())
    }

    @Test
    fun producerLimiter_isReusedWhenMaxInFlightUnchanged() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 2
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)
        assertTrue(helper.sendMessage("bind-out-0", "a"))
        val first = limiterOf(helper)
        assertTrue(helper.sendMessage("bind-out-0", "b"))

        assertSame(first, limiterOf(helper))
    }

    @Test
    fun producerLimiter_doubleCheckReturnsLimiterInstalledWhileWaitingForLock() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 5
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)
        val preInstalled = Semaphore(5)
        var result = false
        val worker = Thread { result = helper.sendMessage("bind-out-0", "data") }

        // Deterministic interleaving: hold the helper monitor so the worker passes the
        // unsynchronized fast path (limiter still null) and parks on synchronized(this);
        // BLOCKED state proves it is past the fast path. Installing the limiter now forces
        // the synchronized double-check branch to return the pre-installed instance.
        synchronized(helper) {
            worker.start()
            val deadline = System.currentTimeMillis() + 10_000
            while (worker.state != Thread.State.BLOCKED) {
                if (System.currentTimeMillis() > deadline) {
                    kotlin.test.fail("worker did not reach the limiter lock in time")
                }
                Thread.onSpinWait()
            }
            inject(helper, "producerLimiter", preInstalled)
            StreamProducerHelper::class.java.getDeclaredField("producerLimiterSize").apply {
                isAccessible = true
                setInt(helper, 5)
            }
        }
        worker.join(10_000)

        assertTrue(result)
        assertSame(preInstalled, limiterOf(helper))
        assertEquals(5, preInstalled.availablePermits(), "permit must be released after the send")
    }

    @Test
    fun producerLimiter_coercesNonPositiveMaxInFlightToOne() {
        val limit = StreamProducerLimitProperties().apply {
            enabled = true
            maxInFlight = 0
        }
        val helper = newHelper(limit = limit)
        stubBridge(true)

        assertTrue(helper.sendMessage("bind-out-0", "data"))

        assertEquals(1, limiterOf(helper).availablePermits())
    }

    // ----- helpers -----

    private fun newHelper(
        executor: ThreadPoolTaskExecutor = SyncExecutor(),
        limit: StreamProducerLimitProperties? = null,
        bindings: Map<String, String> = mapOf("bind-out-0" to "topic-a")
    ): StreamProducerHelper {
        bridge = mock(StreamBridge::class.java)
        channel = RecordingChannel()
        val props = BindingServiceProperties()
        props.bindings = bindings.mapValues { (_, dest) -> BindingProperties().apply { destination = dest } }
        val helper = StreamProducerHelper()
        inject(helper, "streamBridge", bridge)
        inject(helper, "properties", props)
        inject(helper, "streamAsyncSendExecutor", executor)
        inject(helper, "mqProducerChannel", channel)
        inject(helper, "producerLimitProperties", limit)
        return helper
    }

    private fun inject(helper: StreamProducerHelper, fieldName: String, value: Any?) {
        StreamProducerHelper::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(helper, value)
        }
    }

    private fun limiterOf(helper: StreamProducerHelper): Semaphore =
        StreamProducerHelper::class.java.getDeclaredField("producerLimiter").run {
            isAccessible = true
            get(helper) as Semaphore
        }

    private fun stubBridge(result: Boolean) {
        `when`(bridge.send(anyString(), any(Any::class.java), any(MimeType::class.java))).thenReturn(result)
    }

    private fun stubBridgeThrows(t: Throwable) {
        `when`(bridge.send(anyString(), any(Any::class.java), any(MimeType::class.java))).thenThrow(t)
    }

    private fun capturedMessage(bindingName: String): Message<*> {
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(bridge).send(eq(bindingName), captor.capture(), eq(StreamMessageConverter.MESSAGE_TYPE))
        return assertIs<Message<*>>(captor.value)
    }

    /** Executor that runs submitted tasks synchronously to keep tests deterministic. */
    private class SyncExecutor : ThreadPoolTaskExecutor() {
        override fun execute(task: Runnable) = task.run()
    }

    /** Executor that always rejects submitted tasks. */
    private class RejectingExecutor : ThreadPoolTaskExecutor() {
        override fun execute(task: Runnable): Unit = throw RejectedExecutionException("rejected")
    }

    private class RecordingChannel : MessageChannel {
        val messages = mutableListOf<Message<*>>()
        override fun send(message: Message<*>, timeout: Long): Boolean {
            messages.add(message)
            return true
        }
    }

    private class TestFailHandler(private val bindingName: String) : IStreamFailHandler {
        override fun bindName(): String = bindingName
        override fun persistFailedData(data: StreamProducerMsgVo): String = bindingName
    }

}
