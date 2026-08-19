package io.kudos.ability.distributed.tx.seata.http

import jakarta.servlet.FilterChain
import org.apache.seata.core.context.RootContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [SeataXidServletFilter] inbound XID binding and cleanup.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class SeataXidServletFilterTest {

    @AfterTest
    fun cleanup() {
        if (RootContext.getXID() != null) {
            RootContext.unbind()
        }
    }

    @Test
    fun doFilter_bindsIncomingXidForRequestAndUnbindsAfterwards() {
        val request = MockHttpServletRequest().apply {
            addHeader(RootContext.KEY_XID, "incoming-xid")
        }
        val response = MockHttpServletResponse()
        var xidDuringChain: String? = null
        val chain = FilterChain { _, _ ->
            xidDuringChain = RootContext.getXID()
        }

        SeataXidServletFilter().doFilter(request, response, chain)

        assertEquals("incoming-xid", xidDuringChain)
        assertNull(RootContext.getXID())
    }

    @Test
    fun doFilter_ignoresMissingXidHeader() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var chainInvoked = false
        var xidDuringChain: String? = "sentinel"
        val chain = FilterChain { _, _ ->
            chainInvoked = true
            xidDuringChain = RootContext.getXID()
        }

        SeataXidServletFilter().doFilter(request, response, chain)

        kotlin.test.assertTrue(chainInvoked)
        assertNull(xidDuringChain)
        assertNull(RootContext.getXID())
    }

    @Test
    fun doFilter_ignoresBlankXidHeader() {
        val request = MockHttpServletRequest().apply {
            addHeader(RootContext.KEY_XID, "   ")
        }
        val response = MockHttpServletResponse()
        var xidDuringChain: String? = "sentinel"
        val chain = FilterChain { _, _ ->
            xidDuringChain = RootContext.getXID()
        }

        SeataXidServletFilter().doFilter(request, response, chain)

        assertNull(xidDuringChain)
        assertNull(RootContext.getXID())
    }

    @Test
    fun doFilter_unbindsEvenWhenChainThrows() {
        val request = MockHttpServletRequest().apply {
            addHeader(RootContext.KEY_XID, "incoming-xid")
        }
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ ->
            throw IllegalStateException("downstream failure")
        }

        kotlin.test.assertFailsWith<IllegalStateException> {
            SeataXidServletFilter().doFilter(request, response, chain)
        }
        assertNull(RootContext.getXID(), "XID must be unbound even on exception to avoid thread-pool pollution")
    }

    @Test
    fun doFilter_bindsTypicalSeataXidFormatVerbatim() {
        // Typical Seata XID format: "ip:port:transactionId" — must be bound without any mangling.
        val request = MockHttpServletRequest().apply {
            addHeader(RootContext.KEY_XID, "10.0.0.7:8091:9007199254740993")
        }
        val response = MockHttpServletResponse()
        var xidDuringChain: String? = null
        val chain = FilterChain { _, _ ->
            xidDuringChain = RootContext.getXID()
        }

        SeataXidServletFilter().doFilter(request, response, chain)

        assertEquals("10.0.0.7:8091:9007199254740993", xidDuringChain)
        assertNull(RootContext.getXID())
    }

    @Test
    fun doFilter_doesNotOverrideExistingXid() {
        RootContext.bind("existing-xid")
        val request = MockHttpServletRequest().apply {
            addHeader(RootContext.KEY_XID, "incoming-xid")
        }
        val response = MockHttpServletResponse()
        var xidDuringChain: String? = null
        val chain = FilterChain { _, _ ->
            xidDuringChain = RootContext.getXID()
        }

        SeataXidServletFilter().doFilter(request, response, chain)

        assertEquals("existing-xid", xidDuringChain)
        assertEquals("existing-xid", RootContext.getXID())
    }
}
