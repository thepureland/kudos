package io.kudos.ability.distributed.tx.seata.feign

import jakarta.servlet.FilterChain
import org.apache.seata.core.context.RootContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
