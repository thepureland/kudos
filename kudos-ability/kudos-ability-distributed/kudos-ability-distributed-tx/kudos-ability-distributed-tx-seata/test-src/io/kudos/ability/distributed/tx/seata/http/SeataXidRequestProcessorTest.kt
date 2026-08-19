package io.kudos.ability.distributed.tx.seata.http

import io.kudos.context.core.KudosContext
import org.apache.seata.core.context.RootContext
import org.springframework.http.HttpMethod
import org.springframework.mock.http.client.MockClientHttpRequest
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests that [SeataXidRequestProcessor] propagates the XID request header.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SeataXidRequestProcessorTest {

    @AfterTest
    fun cleanup() {
        if (RootContext.getXID() != null) {
            RootContext.unbind()
        }
    }

    @Test
    fun processContext_writesXidHeaderWhenRootContextHasXid() {
        RootContext.bind("xid-123")
        val request = newRequest()

        SeataXidRequestProcessor().processContext(request, KudosContext())

        assertEquals("xid-123", request.headers.getFirst(RootContext.KEY_XID))
    }

    @Test
    fun processContext_doesNotWriteHeaderWithoutXid() {
        val request = newRequest()

        SeataXidRequestProcessor().processContext(request, KudosContext())

        assertNull(request.headers.getFirst(RootContext.KEY_XID))
    }

    @Test
    fun processContext_preservesExistingHeadersAndSupportsTypicalXidFormat() {
        // Typical Seata XID format: "ip:port:transactionId"
        RootContext.bind("192.168.0.1:8091:1234567890")
        val request = newRequest().apply { headers.add("X-Custom", "keep-me") }

        SeataXidRequestProcessor().processContext(request, KudosContext())

        assertEquals("keep-me", request.headers.getFirst("X-Custom"))
        assertEquals("192.168.0.1:8091:1234567890", request.headers.getFirst(RootContext.KEY_XID))
    }

    private fun newRequest() = MockClientHttpRequest(HttpMethod.GET, URI.create("http://ms12/api/x"))
}
