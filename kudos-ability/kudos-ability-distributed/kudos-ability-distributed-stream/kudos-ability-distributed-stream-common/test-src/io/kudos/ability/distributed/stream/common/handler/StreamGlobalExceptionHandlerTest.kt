package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.common.model.vo.StreamHeader
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.base.lang.SerializationKit
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.ErrorMessage
import org.springframework.messaging.support.GenericMessage
import java.io.Serializable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [StreamGlobalExceptionHandler]:
 * - consumer-side routing in `globalHandleError` (save-exception switch, non-MessageHandlingException
 *   payload, null failedMessage, non-consumer headers, happy persistence path, swallowed failures)
 * - producer-side routing in `handleProducerError` / `handSyncChannelError` (failedMessage vs
 *   originalMessage fallback, consumer re-check, missing SCST binding header, non-StreamMessageVo
 *   body, registered-handler persistence, null-body class name, swallowed handler errors)
 *
 * Collaborators are injected via reflection; the global fail-handler registry is snapshotted and
 * restored around each test.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamGlobalExceptionHandlerTest {

    private lateinit var snapshot: Map<String, IStreamFailHandler>

    @BeforeTest
    fun saveRegistry() {
        snapshot = StreamFailHandlerRegistryTestOps.snapshot()
        StreamFailHandlerRegistryTestOps.clear()
    }

    @AfterTest
    fun restoreRegistry() {
        StreamFailHandlerRegistryTestOps.restore(snapshot)
    }

    // ----- consumer side: globalHandleError -----

    @Test
    fun globalHandleError_skipsWhenSaveExceptionDisabled() {
        val service = mock(ISysMqFailMsgService::class.java)
        val handler = newHandler(service, saveException = false)

        handler.globalHandleError(ErrorMessage(MessageHandlingException(consumerMessage())))

        verifyNoInteractions(service)
    }

    @Test
    fun globalHandleError_skipsWhenPayloadIsNotMessageHandlingException() {
        val service = mock(ISysMqFailMsgService::class.java)
        val handler = newHandler(service)

        handler.globalHandleError(ErrorMessage(RuntimeException("not a handling exception")))

        verifyNoInteractions(service)
    }

    @Test
    fun globalHandleError_skipsWhenFailedMessageIsNull() {
        val service = mock(ISysMqFailMsgService::class.java)
        val handler = newHandler(service)
        // The constructor no longer accepts a null failed message, so mock the accessor instead.
        val exception = mock(MessageHandlingException::class.java)
        `when`(exception.failedMessage).thenReturn(null)

        handler.globalHandleError(ErrorMessage(exception))

        verifyNoInteractions(service)
    }

    @Test
    fun globalHandleError_skipsWhenMessageIsNotFromConsumer() {
        val service = mock(ISysMqFailMsgService::class.java)
        val handler = newHandler(service)
        val message = GenericMessage(
            SerializationKit.serialize("body"),
            mapOf<String, Any>("plainHeader" to "x")
        )

        handler.globalHandleError(ErrorMessage(MessageHandlingException(message)))

        verifyNoInteractions(service)
    }

    @Test
    fun globalHandleError_persistsConsumerExceptionMessage() {
        val service = mock(ISysMqFailMsgService::class.java)
        `when`(service.save(anyNonNull(SysMqFailMsg::class.java))).thenReturn(true)
        val handler = newHandler(service)

        handler.globalHandleError(ErrorMessage(MessageHandlingException(consumerMessage("topic-x", "body-1"))))

        // Explicit type: forClass returns a flexible type whose null check would trip on capture().
        val captor: ArgumentCaptor<SysMqFailMsg> = ArgumentCaptor.forClass(SysMqFailMsg::class.java)
        verify(service).save(captureNonNull(captor))
        val saved = captor.value
        assertEquals("topic-x", saved.topic)
        assertTrue(saved.msgBodyJson.contains("body-1"), "body json should contain the payload: ${saved.msgBodyJson}")
        assertTrue(saved.msgHeaderJson.isNotBlank(), "header json must not be blank")
        assertNotNull(saved.createTime)
    }

    @Test
    fun globalHandleError_recognizesAmqpAndRocketHeadersCaseInsensitively() {
        val service = mock(ISysMqFailMsgService::class.java)
        `when`(service.save(anyNonNull(SysMqFailMsg::class.java))).thenReturn(true)
        val handler = newHandler(service)
        val message = GenericMessage(
            SerializationKit.serialize("body-amqp"),
            mapOf<String, Any>(
                "AMQP_receivedRoutingKey" to "rk",
                StreamHeader.TOPIC_KEY to "topic-amqp"
            )
        )

        handler.globalHandleError(ErrorMessage(MessageHandlingException(message)))

        verify(service).save(anyNonNull(SysMqFailMsg::class.java))
    }

    @Test
    fun globalHandleError_swallowsPersistenceFailures() {
        val service = mock(ISysMqFailMsgService::class.java)
        val handler = newHandler(service)
        // Payload is a String, not a ByteArray -> ClassCastException inside the try block.
        val message = GenericMessage(
            "not-bytes",
            mapOf<String, Any>("kafka_receivedTopic" to "t", StreamHeader.TOPIC_KEY to "topic-x")
        )

        // Must not throw.
        handler.globalHandleError(ErrorMessage(MessageHandlingException(message)))

        verifyNoInteractions(service)
    }

    // ----- producer side: handleProducerError / handSyncChannelError -----

    @Test
    fun handleProducerError_persistsViaRegisteredHandler() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler("bind-1")
        StreamFailHandlerItem.put("bind-1", recording)
        val message = producerMessage("bind-1", StreamMessageVo("data-1"))

        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))

        val vo = assertNotNull(recording.lastData, "handler must receive the failed message")
        assertEquals("bind-1", vo.bindName)
        assertEquals("java.lang.String", vo.msgBodyClassName)
        assertTrue(vo.msgBodyJson!!.contains("data-1"), "body json should contain payload: ${vo.msgBodyJson}")
        assertTrue(vo.msgHeaderJson!!.isNotBlank())
    }

    @Test
    fun handSyncChannelError_persistsViaRegisteredHandler() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler("bind-sync")
        StreamFailHandlerItem.put("bind-sync", recording)
        val message = producerMessage("bind-sync", StreamMessageVo("data-sync"))

        handler.handSyncChannelError(ErrorMessage(MessageHandlingException(message)))

        assertEquals("bind-sync", assertNotNull(recording.lastData).bindName)
    }

    @Test
    fun handleProducerError_fallsBackToOriginalMessageWhenPayloadIsPlainThrowable() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler("bind-2")
        StreamFailHandlerItem.put("bind-2", recording)
        val original = producerMessage("bind-2", StreamMessageVo("data-2"))

        handler.handleProducerError(ErrorMessage(RuntimeException("send failed"), original))

        assertEquals("bind-2", assertNotNull(recording.lastData).bindName)
    }

    @Test
    fun handleProducerError_ignoresWhenNoFailedAndNoOriginalMessage() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler(IStreamFailHandler.DEFAULT_BIND_NAME)
        StreamFailHandlerItem.put(IStreamFailHandler.DEFAULT_BIND_NAME, recording)

        handler.handleProducerError(ErrorMessage(RuntimeException("no message at all")))

        assertNull(recording.lastData)
    }

    @Test
    fun handleProducerError_ignoresConsumerSideMessages() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler(IStreamFailHandler.DEFAULT_BIND_NAME)
        StreamFailHandlerItem.put(IStreamFailHandler.DEFAULT_BIND_NAME, recording)
        val message = GenericMessage(
            SerializationKit.serialize(StreamMessageVo("x")),
            mapOf<String, Any>(
                "rocket_queueId" to 1,
                StreamHeader.SCST_BIND_NAME to "bind-3"
            )
        )

        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))

        assertNull(recording.lastData)
    }

    @Test
    fun handleProducerError_ignoresWhenBindingHeaderIsMissing() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler(IStreamFailHandler.DEFAULT_BIND_NAME)
        StreamFailHandlerItem.put(IStreamFailHandler.DEFAULT_BIND_NAME, recording)
        val message = GenericMessage(
            SerializationKit.serialize(StreamMessageVo("x")),
            mapOf<String, Any>("custom" to "y")
        )

        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))

        assertNull(recording.lastData)
    }

    @Test
    fun handleProducerError_ignoresWhenBodyIsNotStreamMessageVo() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler("bind-4")
        StreamFailHandlerItem.put("bind-4", recording)
        val message = GenericMessage(
            SerializationKit.serialize("plain-string-body" as Serializable),
            mapOf<String, Any>(StreamHeader.SCST_BIND_NAME to "bind-4")
        )

        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))

        assertNull(recording.lastData)
    }

    @Test
    fun handleProducerError_handlesNullBodyDataWithoutClassName() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler("bind-5")
        StreamFailHandlerItem.put("bind-5", recording)
        val message = producerMessage("bind-5", StreamMessageVo<String>(null))

        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))

        val vo = assertNotNull(recording.lastData)
        assertNull(vo.msgBodyClassName, "null payload must not have a body class name")
    }

    @Test
    fun handleProducerError_doesNothingWhenNoHandlerRegisteredAtAll() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val message = producerMessage("bind-none", StreamMessageVo("data"))

        // Registry is empty -> StreamFailHandlerItem.get returns null; must not throw.
        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))
    }

    @Test
    fun handleProducerError_swallowsProcessingErrors() {
        val handler = newHandler(mock(ISysMqFailMsgService::class.java))
        val recording = RecordingFailHandler("bind-err")
        StreamFailHandlerItem.put("bind-err", recording)
        // Payload is not a ByteArray -> ClassCastException, must be swallowed.
        val message = GenericMessage(
            "not-bytes",
            mapOf<String, Any>(StreamHeader.SCST_BIND_NAME to "bind-err")
        )

        handler.handleProducerError(ErrorMessage(MessageHandlingException(message)))

        assertNull(recording.lastData)
    }

    // ----- helpers -----

    /** Mockito's any()/capture() return null; the unchecked cast bypasses Kotlin's platform-type null check. */
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(type: Class<T>): T = any(type) as T

    @Suppress("UNCHECKED_CAST")
    private fun <T> captureNonNull(captor: ArgumentCaptor<T>): T = captor.capture() as T

    private fun newHandler(
        service: ISysMqFailMsgService,
        saveException: Boolean = true
    ): StreamGlobalExceptionHandler {
        val handler = StreamGlobalExceptionHandler()
        StreamGlobalExceptionHandler::class.java.getDeclaredField("saveException").apply {
            isAccessible = true
            setBoolean(handler, saveException)
        }
        StreamGlobalExceptionHandler::class.java.getDeclaredField("streamExceptionService").apply {
            isAccessible = true
            set(handler, service)
        }
        return handler
    }

    private fun consumerMessage(topic: String = "topic-x", body: String = "body"): Message<ByteArray> =
        GenericMessage(
            SerializationKit.serialize(body),
            mapOf<String, Any>(
                "kafka_receivedTopic" to topic,
                StreamHeader.TOPIC_KEY to topic
            )
        )

    private fun producerMessage(bindName: String, body: StreamMessageVo<*>): Message<ByteArray> =
        GenericMessage(
            SerializationKit.serialize(body),
            mapOf<String, Any>(StreamHeader.SCST_BIND_NAME to bindName)
        )

    private class RecordingFailHandler(private val bindingName: String) : IStreamFailHandler {
        var lastData: StreamProducerMsgVo? = null

        override fun bindName(): String = bindingName

        override fun persistFailedData(data: StreamProducerMsgVo): String {
            lastData = data
            return "recorded"
        }
    }

}
