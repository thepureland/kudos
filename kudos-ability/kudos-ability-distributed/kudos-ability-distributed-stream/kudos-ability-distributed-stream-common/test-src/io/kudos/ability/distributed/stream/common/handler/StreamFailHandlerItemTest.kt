package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the [StreamFailHandlerItem] registry: exact-binding lookup, fallback to the default
 * handler, registry-miss returning null, and [StreamFailHandlerItem.hasFailedHandler] checks.
 * The global registry is snapshotted and restored around each test for order independence.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamFailHandlerItemTest {

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

    @Test
    fun get_returnsExactBindingHandlerBeforeDefaultHandler() {
        val defaultHandler = TestFailHandler(IStreamFailHandler.DEFAULT_BIND_NAME)
        val exactHandler = TestFailHandler("order-out-0")

        StreamFailHandlerItem.put(defaultHandler.bindName().orEmpty(), defaultHandler)
        StreamFailHandlerItem.put(exactHandler.bindName().orEmpty(), exactHandler)

        assertSame(exactHandler, StreamFailHandlerItem.get("order-out-0"))
    }

    @Test
    fun get_fallsBackToDefaultHandlerWhenBindingIsMissing() {
        val defaultHandler = TestFailHandler(IStreamFailHandler.DEFAULT_BIND_NAME)

        StreamFailHandlerItem.put(defaultHandler.bindName().orEmpty(), defaultHandler)

        assertTrue(StreamFailHandlerItem.hasFailedHandler(IStreamFailHandler.DEFAULT_BIND_NAME))
        assertSame(defaultHandler, StreamFailHandlerItem.get("missing-out-0"))
    }

    @Test
    fun get_returnsNullWhenNeitherExactNorDefaultHandlerExists() {
        assertFalse(StreamFailHandlerItem.hasFailedHandler("missing-out-0"))
        assertNull(StreamFailHandlerItem.get("missing-out-0"))
    }

    @Test
    fun put_overwritesExistingHandlerForSameBinding() {
        val first = TestFailHandler("dup-out-0")
        val second = TestFailHandler("dup-out-0")

        StreamFailHandlerItem.put("dup-out-0", first)
        StreamFailHandlerItem.put("dup-out-0", second)

        assertSame(second, StreamFailHandlerItem.get("dup-out-0"))
    }

    @Test
    fun constants_keepWireCompatibleValues() {
        kotlin.test.assertEquals("_s_default_", IStreamFailHandler.DEFAULT_BIND_NAME)
        kotlin.test.assertEquals("mqProducerChannel", IStreamFailHandler.CHANNEL_BEN_NAME)
    }

    private class TestFailHandler(private val bindingName: String) : IStreamFailHandler {
        override fun bindName(): String = bindingName

        override fun persistFailedData(data: StreamProducerMsgVo): String = bindingName
    }

}
