package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.ability.distributed.discovery.nacos.support.IFeignProviderContextProcess
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.support.StaticApplicationContext
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


internal class FeignContextWebFilterTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        KudosContextHolder.clear()
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
        ctx.close()
    }

    @Test
    fun doFilter_withoutFeignOrNotifyMarker_doesNotModifyContext() {
        val context = KudosContextHolder.get().apply {
            tenantId = "existing-tenant"
            traceKey = "existing-trace"
        }
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.TENANT_ID, "incoming-tenant")
            addHeader(Consts.RequestHeader.TRACE_KEY, "incoming-trace")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals("existing-tenant", context.tenantId)
        assertEquals("existing-trace", context.traceKey)
    }

    @Test
    fun doFilter_withFeignMarker_writesHeadersBackToKudosContext() {
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-a")
            addHeader(Consts.RequestHeader.SUB_SYS_CODE, "sys-a")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-a")
            addHeader(Consts.RequestHeader.DATASOURCE_ID, "ds-a")
            addHeader(Consts.RequestHeader.LOCAL, "en_US")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val context = KudosContextHolder.get()
        assertEquals("tenant-a", context.tenantId)
        assertEquals("sys-a", context.subSystemCode)
        assertEquals("trace-a", context.traceKey)
        assertEquals("ds-a", context.dataSourceId)
        assertEquals(Locale.US, context.clientInfo?.locale)
    }

    @Test
    fun doFilter_allowUnmarkedContextHeaders_writesHeadersWithoutMarker() {
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-dev")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-dev")
        }

        FeignContextWebFilter(allowUnmarkedContextHeaders = true)
            .doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val context = KudosContextHolder.get()
        assertEquals("tenant-dev", context.tenantId)
        assertEquals("trace-dev", context.traceKey)
    }

    @Test
    fun doFilter_invokesProviderContextProcessor() {
        val processor = RecordingProviderContextProcess()
        ctx.beanFactory.registerSingleton("recordingProviderContextProcess", processor)
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.NOTIFY_REQUEST, "true")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals(1, processor.calls)
        assertEquals("from-processor", KudosContextHolder.get().subSystemCode)
    }

    private class RecordingProviderContextProcess : IFeignProviderContextProcess {
        var calls = 0

        override fun processContext(request: HttpServletRequest, context: KudosContext) {
            calls++
            context.subSystemCode = "from-processor"
        }
    }

}
