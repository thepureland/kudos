package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue


internal class StreamFailHandlerItemTest {

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

    private class TestFailHandler(private val bindingName: String) : IStreamFailHandler {
        override fun bindName(): String = bindingName

        override fun persistFailedData(data: StreamProducerMsgVo): String = bindingName
    }

}
