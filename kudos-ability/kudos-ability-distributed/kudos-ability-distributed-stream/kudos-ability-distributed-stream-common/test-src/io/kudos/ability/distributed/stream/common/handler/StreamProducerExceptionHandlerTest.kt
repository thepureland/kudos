package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.base.data.json.JsonKit
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.retry.RetryConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.modules.SerializersModule
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.messaging.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [StreamProducerExceptionHandler]:
 * - [StreamProducerExceptionHandler.readMessageBody]: restore by persisted class name
 *   (kotlinx-serializable types), primary-constructor fallback with numeric/boolean/string
 *   coercion for non-serializable types, unknown class name falling back to dynamic JSON,
 *   blank class name, non-map dynamic body returning null, class without a primary constructor,
 *   and dynamic JSON unwrapping of nested objects / arrays / primitives.
 * - [StreamProducerExceptionHandler.processFailedData]: happy path rebuilding the Message and
 *   delegating to StreamProducerHelper.doRealSend (requires registering a contextual Any
 *   serializer — see the in-class note about the suspected main-code bug), the propagated send
 *   result, and require() failures for null bindName / msgBodyJson / msgHeaderJson,
 *   undeserializable body and header JSON.
 * - filePath(): configured-path priority, RetryConfig fallback, atomicServiceCode vs "default".
 * - businessType / cronExpression / bindName constants.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamProducerExceptionHandlerTest {

    @AfterTest
    fun tearDown() {
        KudosContextHolder.clear()
    }

    // ----- readMessageBody -----

    @Test
    fun readMessageBody_restoresPayloadByPersistedClassName() {
        val handler = StreamProducerExceptionHandler()
        val payload = RetryPayload("order-1", 3)

        val restored = handler.readMessageBody(JsonKit.toJson(payload), RetryPayload::class.java.name)

        assertEquals(payload, assertIs<RetryPayload>(restored))
    }

    @Test
    fun readMessageBody_fallsBackToDynamicJsonWhenClassNameIsMissing() {
        val handler = StreamProducerExceptionHandler()

        val restored = handler.readMessageBody("""{"id":"order-2","count":5}""", null)

        val map = assertIs<Map<*, *>>(restored)
        assertEquals("order-2", map["id"])
        assertEquals(5, map["count"])
    }

    @Test
    fun readMessageBody_fallsBackToDynamicJsonWhenClassNameIsBlank() {
        val handler = StreamProducerExceptionHandler()

        val restored = handler.readMessageBody("""{"id":"order-3"}""", "   ")

        assertEquals("order-3", assertIs<Map<*, *>>(restored)["id"])
    }

    @Test
    fun readMessageBody_restoresNonSerializableClassByPrimaryConstructorWithCoercion() {
        val handler = StreamProducerExceptionHandler()
        val json = """{"i":42,"l":9876543210,"d":2.5,"f":1.25,"sh":7,"b":3,"bool":true,"s":"text","tags":["x","y"],"extra":"ignored"}"""

        val restored = handler.readMessageBody(json, CoercePayload::class.java.name)

        val payload = assertIs<CoercePayload>(restored)
        assertEquals(42, payload.i)
        assertEquals(9876543210L, payload.l)
        assertEquals(2.5, payload.d)
        assertEquals(1.25f, payload.f)
        assertEquals(7.toShort(), payload.sh)
        assertEquals(3.toByte(), payload.b)
        assertEquals(true, payload.bool)
        assertEquals("text", payload.s)
        assertEquals(listOf("x", "y"), payload.tags)
        assertEquals("dflt", payload.withDefault, "parameters absent from JSON must keep their defaults")
    }

    @Test
    fun readMessageBody_fallsBackToDynamicJsonWhenClassNameIsUnknown() {
        val handler = StreamProducerExceptionHandler()

        val restored = handler.readMessageBody("""{"k":"v"}""", "no.such.clazz.Missing")

        assertEquals("v", assertIs<Map<*, *>>(restored)["k"])
    }

    @Test
    fun readMessageBody_returnsNullWhenDynamicBodyIsNotMapForKnownClass() {
        val handler = StreamProducerExceptionHandler()

        // JSON array cannot feed a primary constructor -> restore returns null.
        assertNull(handler.readMessageBody("[1,2,3]", CoercePayload::class.java.name))
    }

    @Test
    fun readMessageBody_returnsNullWhenClassHasNoPrimaryConstructor() {
        val handler = StreamProducerExceptionHandler()

        assertNull(handler.readMessageBody("""{"v":1}""", NoPrimaryCtorPayload::class.java.name))
    }

    @Test
    fun readMessageBody_unwrapsNestedDynamicJsonStructures() {
        val handler = StreamProducerExceptionHandler()
        val json =
            """{"nested":{"k":"v"},"arr":[1,"two",false],"big":9876543210,"pi":3.14,"flag":true,"name":"kudos"}"""

        val map = assertIs<Map<*, *>>(handler.readMessageBody(json, null))

        assertEquals(mapOf("k" to "v"), map["nested"])
        assertEquals(listOf(1, "two", false), map["arr"])
        assertEquals(9876543210L, map["big"])
        assertEquals(3.14, map["pi"])
        assertEquals(true, map["flag"])
        assertEquals("kudos", map["name"])
    }

    // ----- processFailedData -----
    //
    // NOTE (suspected main-code bug): with the default JsonKit configuration the happy path of
    // processFailedData is unreachable — `JsonKit.fromJson<MutableMap<String, Any>>(msgHeaderJson)`
    // always returns null because kotlinx-serialization has no serializer for `Any`, so even a
    // perfectly valid header JSON ends in "Failed to deserialize msgHeaderJson". The success-path
    // tests below therefore register a contextual Any serializer through the public
    // JsonKit.registerSerializersModule API (and clean it up afterwards), which is the only
    // configuration under which the retry-resend path can work today.

    @Test
    fun processFailedData_rebuildsMessageAndResendsWithResendFlag() {
        withContextualAnySerializer {
            val helper = mock(StreamProducerHelper::class.java)
            `when`(helper.doRealSend(anyString(), anyNonNull<Message<StreamMessageVo<Any?>>>(), anyBoolean()))
                .thenReturn(true)
            val handler = newHandler(helper = helper)
            val data = StreamProducerMsgVo().apply {
                bindName = "orders-out-0"
                msgBodyJson = """{"id":"o-1"}"""
                msgHeaderJson = """{"custom":"h-1"}"""
            }

            assertTrue(handler.processFailedDataForTest(data))

            @Suppress("UNCHECKED_CAST")
            val captor: ArgumentCaptor<Message<StreamMessageVo<Any?>>> =
                ArgumentCaptor.forClass(Message::class.java) as ArgumentCaptor<Message<StreamMessageVo<Any?>>>
            verify(helper).doRealSend(anyString(), captureNonNull(captor), anyBoolean())
            val message = captor.value
            val body = assertIs<StreamMessageVo<*>>(message.payload)
            assertEquals(mapOf("id" to "o-1"), body.data)
            assertEquals("h-1", message.headers["custom"])
        }
    }

    @Test
    fun processFailedData_propagatesFalseSendResult() {
        withContextualAnySerializer {
            val helper = mock(StreamProducerHelper::class.java)
            `when`(helper.doRealSend(anyString(), anyNonNull<Message<StreamMessageVo<Any?>>>(), anyBoolean()))
                .thenReturn(false)
            val handler = newHandler(helper = helper)
            val data = StreamProducerMsgVo().apply {
                bindName = "orders-out-0"
                msgBodyJson = """{"id":"o-2"}"""
                msgHeaderJson = """{"custom":"h-2"}"""
            }

            assertFalse(handler.processFailedDataForTest(data))
        }
    }

    @Test
    fun processFailedData_failsWhenBindNameIsNull() {
        val handler = newHandler()
        val data = StreamProducerMsgVo().apply {
            msgBodyJson = "{}"
            msgHeaderJson = "{}"
        }

        val e = assertFailsWith<IllegalArgumentException> { handler.processFailedDataForTest(data) }
        assertEquals("bindName must not be null", e.message)
    }

    @Test
    fun processFailedData_failsWhenMsgBodyJsonIsNull() {
        val handler = newHandler()
        val data = StreamProducerMsgVo().apply {
            bindName = "b"
            msgHeaderJson = "{}"
        }

        val e = assertFailsWith<IllegalArgumentException> { handler.processFailedDataForTest(data) }
        assertEquals("msgBodyJson must not be null", e.message)
    }

    @Test
    fun processFailedData_failsWhenMsgHeaderJsonIsNull() {
        val handler = newHandler()
        val data = StreamProducerMsgVo().apply {
            bindName = "b"
            msgBodyJson = "{}"
        }

        val e = assertFailsWith<IllegalArgumentException> { handler.processFailedDataForTest(data) }
        assertEquals("msgHeaderJson must not be null", e.message)
    }

    @Test
    fun processFailedData_failsWhenBodyCannotBeDeserialized() {
        val handler = newHandler()
        val data = StreamProducerMsgVo().apply {
            bindName = "b"
            msgBodyJson = """{"v":1}"""
            msgBodyClassName = NoPrimaryCtorPayload::class.java.name
            msgHeaderJson = "{}"
        }

        val e = assertFailsWith<IllegalArgumentException> { handler.processFailedDataForTest(data) }
        assertEquals("Failed to deserialize msgBodyJson", e.message)
    }

    @Test
    fun processFailedData_failsWhenHeaderJsonIsInvalid() {
        val handler = newHandler()
        val data = StreamProducerMsgVo().apply {
            bindName = "b"
            msgBodyJson = """{"id":"x"}"""
            msgHeaderJson = "not json at all"
        }

        val e = assertFailsWith<IllegalArgumentException> { handler.processFailedDataForTest(data) }
        assertEquals("Failed to deserialize msgHeaderJson", e.message)
    }

    // ----- filePath / metadata -----

    @Test
    fun filePath_prefersConfiguredPathAndAtomicServiceCode() {
        val handler = newHandler(configuredPath = "C:/tmp/stream-fail")
        KudosContextHolder.set(KudosContext().apply { atomicServiceCode = "svc-a" })

        assertEquals("C:/tmp/stream-fail/svc-a", handler.filePath())
    }

    @Test
    fun filePath_fallsBackToRetryConfigAndDefaultServiceCode() {
        val handler = newHandler()
        KudosContextHolder.clear()

        assertEquals("${RetryConfig.baseFailedDataPath}/default", handler.filePath())
    }

    @Test
    fun metadata_keepsDocumentedConstants() {
        val handler = StreamProducerExceptionHandler()
        assertEquals("mq-producer-fail", handler.businessType)
        assertEquals("0 0/1 * * * *", handler.cronExpression)
        assertEquals(IStreamFailHandler.DEFAULT_BIND_NAME, handler.bindName())
    }

    // ----- helpers -----

    /** Mockito's any()/capture() return null; the unchecked cast bypasses Kotlin's platform-type null check. */
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = any<T>() as T

    @Suppress("UNCHECKED_CAST")
    private fun <T> captureNonNull(captor: ArgumentCaptor<T>): T = captor.capture() as T

    /**
     * Registers a contextual Any deserializer via the public [JsonKit.registerSerializersModule] API
     * for the duration of [block], then removes it again reflectively (there is no public unregister)
     * so other tests keep seeing the default JsonKit configuration.
     */
    private fun withContextualAnySerializer(block: () -> Unit) {
        val module = SerializersModule { contextual(Any::class, DynamicAnySerializer) }
        JsonKit.registerSerializersModule(module)
        try {
            block()
        } finally {
            val clazz = Class.forName("io.kudos.base.data.json.JsonAdapters")
            val instance = clazz.getDeclaredField("INSTANCE").run {
                isAccessible = true
                get(null)
            }
            clazz.getDeclaredField("customSerializersModules").run {
                isAccessible = true
                @Suppress("UNCHECKED_CAST")
                (get(instance) as MutableList<SerializersModule>).remove(module)
            }
            listOf("cachedDefaultJson", "cachedPreserveJson").forEach { name ->
                clazz.getDeclaredField(name).run {
                    isAccessible = true
                    set(instance, null)
                }
            }
        }
    }

    /** Test-only contextual deserializer turning arbitrary JSON values into Kotlin primitives / Map / List. */
    private object DynamicAnySerializer : KSerializer<Any> {
        override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

        override fun deserialize(decoder: Decoder): Any {
            val element = (decoder as JsonDecoder).decodeJsonElement()
            return unwrap(element) ?: ""
        }

        override fun serialize(encoder: Encoder, value: Any) =
            throw UnsupportedOperationException("test-only deserializer")

        private fun unwrap(element: JsonElement): Any? = when (element) {
            is JsonObject -> element.mapValues { unwrap(it.value) }
            is JsonArray -> element.map { unwrap(it) }
            JsonNull -> null
            is JsonPrimitive -> element.booleanOrNull
                ?: element.intOrNull
                ?: element.longOrNull
                ?: element.doubleOrNull
                ?: element.content
        }
    }

    private fun newHandler(
        helper: StreamProducerHelper? = null,
        configuredPath: String? = null
    ): StreamProducerExceptionHandler {
        val handler = StreamProducerExceptionHandler()
        if (helper != null) {
            StreamProducerExceptionHandler::class.java.getDeclaredField("streamProducerHelper").apply {
                isAccessible = true
                set(handler, helper)
            }
        }
        if (configuredPath != null) {
            StreamProducerExceptionHandler::class.java.getDeclaredField("configuredFilePath").apply {
                isAccessible = true
                set(handler, configuredPath)
            }
        }
        return handler
    }

    /** Invokes the protected [StreamProducerExceptionHandler.processFailedData], unwrapping reflection exceptions. */
    private fun StreamProducerExceptionHandler.processFailedDataForTest(data: StreamProducerMsgVo): Boolean {
        val method = StreamProducerExceptionHandler::class.java.getDeclaredMethod(
            "processFailedData",
            StreamProducerMsgVo::class.java
        )
        method.isAccessible = true
        try {
            return method.invoke(this, data) as Boolean
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.cause ?: e
        }
    }

}

/**
 * Test DTO for stream producer retry payloads.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Serializable
internal data class RetryPayload(
    val id: String,
    val count: Int
)

/**
 * Non-kotlinx-serializable test DTO exercising the primary-constructor restore path with
 * numeric/boolean/string coercion plus an `else`-branch list parameter and a defaulted parameter.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal data class CoercePayload(
    val i: Int,
    val l: Long,
    val d: Double,
    val f: Float,
    val sh: Short,
    val b: Byte,
    val bool: Boolean,
    val s: String,
    val tags: List<String>,
    val withDefault: String = "dflt"
)

/**
 * Test class without a primary constructor — the constructor-based restore must return null.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Suppress("unused", "UNUSED_PARAMETER", "ConvertSecondaryConstructorToPrimary")
internal class NoPrimaryCtorPayload {
    constructor(v: Int)
}
