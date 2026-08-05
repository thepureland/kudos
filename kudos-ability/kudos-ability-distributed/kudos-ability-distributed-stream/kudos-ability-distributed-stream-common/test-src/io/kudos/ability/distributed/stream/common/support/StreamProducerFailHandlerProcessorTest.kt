package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerRegistryTestOps
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [StreamProducerFailHandlerProcessor]:
 * - non-handler beans pass through untouched and do not register anything
 * - [IStreamFailHandler] beans are registered in [StreamFailHandlerItem] under their bindName
 * - a handler returning null from bindName() fails fast with a message naming the class
 *
 * The global registry is snapshotted and restored around each test.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamProducerFailHandlerProcessorTest {

    private lateinit var snapshot: Map<String, IStreamFailHandler>
    private val processor = StreamProducerFailHandlerProcessor()

    @BeforeTest
    fun saveRegistry() {
        snapshot = StreamFailHandlerRegistryTestOps.snapshot()
        StreamFailHandlerRegistryTestOps.clear()
    }

    @AfterTest
    fun restoreRegistry() {
        StreamFailHandlerRegistryTestOps.restore(snapshot)
    }

    @Test
    fun postProcess_returnsNonHandlerBeanUnchangedWithoutRegistering() {
        val bean = "just a string bean"

        val result = processor.postProcessAfterInitialization(bean, "stringBean")

        assertSame(bean, result)
        assertFalse(StreamFailHandlerItem.hasFailedHandler("stringBean"))
    }

    @Test
    fun postProcess_registersFailHandlerUnderItsBindName() {
        val handler = TestFailHandler("orders-out-0")

        val result = processor.postProcessAfterInitialization(handler, "ordersFailHandler")

        assertSame(handler, result)
        assertTrue(StreamFailHandlerItem.hasFailedHandler("orders-out-0"))
        assertSame(handler, StreamFailHandlerItem.get("orders-out-0"))
    }

    @Test
    fun postProcess_failsFastWhenBindNameIsNull() {
        val handler = TestFailHandler(null)

        val e = assertFailsWith<IllegalArgumentException> {
            processor.postProcessAfterInitialization(handler, "badHandler")
        }
        assertTrue(
            e.message!!.contains(TestFailHandler::class.java.name),
            "error message should name the offending handler class: ${e.message}"
        )
    }

    @Test
    fun constants_documentedBindNameContract() {
        assertEquals("_s_default_", IStreamFailHandler.DEFAULT_BIND_NAME)
    }

    private class TestFailHandler(private val bindingName: String?) : IStreamFailHandler {
        override fun bindName(): String? = bindingName
        override fun persistFailedData(data: StreamProducerMsgVo): String = "persisted"
    }

}
