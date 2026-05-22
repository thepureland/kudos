package io.kudos.ability.distributed.tx.seata.feign

import feign.RequestTemplate
import io.kudos.context.core.KudosContext
import org.apache.seata.core.context.RootContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * [SeataFeignXidProcessor] Feign XID 请求头透传测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class SeataFeignXidProcessorTest {

    @AfterTest
    fun cleanup() {
        if (RootContext.getXID() != null) {
            RootContext.unbind()
        }
    }

    @Test
    fun processContext_writesXidHeaderWhenRootContextHasXid() {
        RootContext.bind("xid-123")
        val template = RequestTemplate()

        SeataFeignXidProcessor().processContext(template, KudosContext())

        assertEquals(listOf("xid-123"), template.headers()[RootContext.KEY_XID]?.toList())
    }

    @Test
    fun processContext_doesNotWriteHeaderWithoutXid() {
        val template = RequestTemplate()

        SeataFeignXidProcessor().processContext(template, KudosContext())

        assertFalse(template.headers().containsKey(RootContext.KEY_XID))
    }
}
