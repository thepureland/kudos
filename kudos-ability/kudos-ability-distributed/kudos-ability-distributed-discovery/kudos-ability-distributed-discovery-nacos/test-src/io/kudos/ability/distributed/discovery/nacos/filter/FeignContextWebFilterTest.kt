package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.ability.distributed.discovery.nacos.support.IFeignProviderContextProcess
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.core.ClientInfo
import io.kudos.context.support.Consts
import jakarta.servlet.ServletRequestWrapper
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for the inbound context-restoration behavior of [FeignContextWebFilter].
 *
 * Coverage:
 *  - requests without the FEIGN_REQUEST / NOTIFY_REQUEST marker pass through untouched
 *  - marked requests write tenant / subsystem / trace / datasource / locale back into KudosContext
 *  - allowUnmarkedContextHeaders=true accepts unmarked requests
 *  - non-HTTP ServletRequest passes straight through (no ClassCastException)
 *  - pre-existing ClientInfo is reused (not replaced)
 *  - locale parsing edge cases: single segment, blank, more than two segments
 *  - blank (whitespace-only) FEIGN_REQUEST / NOTIFY_REQUEST markers are treated as unmarked
 *  - blank (whitespace-only) LOCAL header is ignored
 *  - blank DATASOURCE_ID header does not overwrite the context datasource
 *  - IFeignProviderContextProcess SPI is invoked; a throwing processor does not break the chain
 *  - legacy (no verifier) mode keeps accepting requests on repeated calls (one-time WARN path)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
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
    fun doFilter_blankMarkers_treatedAsUnmarked() {
        val context = KudosContextHolder.get().apply { tenantId = "existing-tenant" }
        val request = MockHttpServletRequest().apply {
            // Whitespace-only markers must NOT activate context restoration (isNullOrBlank gate)
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "   ")
            addHeader(Consts.RequestHeader.NOTIFY_REQUEST, "   ")
            addHeader(Consts.RequestHeader.TENANT_ID, "incoming-tenant")
        }
        val chain = MockFilterChain()

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), chain)

        assertNotNull(chain.request, "the request must still pass down the chain")
        assertEquals("existing-tenant", context.tenantId, "blank markers must not trigger context restoration")
    }

    @Test
    fun doFilter_blankLocaleHeader_isIgnored() {
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.LOCAL, "   ")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val clientInfo = KudosContextHolder.get().clientInfo
        assertNotNull(clientInfo, "ClientInfo must be created even when the locale header is blank")
        assertNull(clientInfo.locale, "a whitespace-only locale header must not be parsed")
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

    @Test
    fun doFilter_nonHttpServletRequest_passesThroughWithoutContextChange() {
        // A plain ServletRequest (not HttpServletRequest) must pass straight through the chain
        val raw = ServletRequestWrapper(MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-raw")
        })
        val chain = MockFilterChain()

        FeignContextWebFilter().doFilter(raw, MockHttpServletResponse(), chain)

        assertSame(raw, chain.request, "non-HTTP request must continue down the chain unchanged")
        assertNull(KudosContextHolder.get().tenantId)
    }

    @Test
    fun doFilter_existingClientInfo_isReusedAndLocaleUpdated() {
        val existingClientInfo = ClientInfo(ClientInfo.Builder())
        KudosContextHolder.get().clientInfo = existingClientInfo
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.LOCAL, "zh_CN")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val context = KudosContextHolder.get()
        assertSame(existingClientInfo, context.clientInfo, "pre-existing ClientInfo must be reused, not replaced")
        assertEquals(Locale.of("zh", "CN"), existingClientInfo.locale)
    }

    @Test
    fun doFilter_singleSegmentLocale_isIgnored() {
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.LOCAL, "en")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val clientInfo = KudosContextHolder.get().clientInfo
        assertNotNull(clientInfo, "ClientInfo must be created even when the locale cannot be parsed")
        assertNull(clientInfo.locale, "a single-segment locale string must not be parsed")
    }

    @Test
    fun doFilter_localeWithEmptySegments_isIgnored() {
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.LOCAL, "_")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNull(KudosContextHolder.get().clientInfo?.locale)
    }

    @Test
    fun doFilter_localeWithMoreThanTwoSegments_usesFirstTwo() {
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.LOCAL, "zh_TW_variant")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals(Locale.of("zh", "TW"), KudosContextHolder.get().clientInfo?.locale)
    }

    @Test
    fun doFilter_blankDataSourceId_doesNotOverwriteContextDataSource() {
        KudosContextHolder.get().dataSourceId = "existing-ds"
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.DATASOURCE_ID, "")
        }

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals("existing-ds", KudosContextHolder.get().dataSourceId)
    }

    @Test
    fun doFilter_throwingProcessor_isLoggedAndChainContinues() {
        ctx.beanFactory.registerSingleton("throwingProviderContextProcess", ThrowingProviderContextProcess())
        val recording = RecordingProviderContextProcess()
        ctx.beanFactory.registerSingleton("recordingProviderContextProcess", recording)
        val request = MockHttpServletRequest().apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-x")
        }
        val chain = MockFilterChain()

        FeignContextWebFilter().doFilter(request, MockHttpServletResponse(), chain)

        assertNotNull(chain.request, "a throwing SPI processor must not break the filter chain")
        assertEquals(1, recording.calls, "remaining processors must still run after one throws")
        assertEquals("tenant-x", KudosContextHolder.get().tenantId)
    }

    @Test
    fun doFilter_legacyUnsignedMode_acceptsRepeatedRequests() {
        // Same filter instance called twice: covers both branches of the one-time WARN guard
        val filter = FeignContextWebFilter()
        repeat(2) { i ->
            KudosContextHolder.clear()
            val request = MockHttpServletRequest().apply {
                addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
                addHeader(Consts.RequestHeader.TENANT_ID, "tenant-$i")
            }
            val chain = MockFilterChain()
            filter.doFilter(request, MockHttpServletResponse(), chain)
            assertNotNull(chain.request)
            assertEquals("tenant-$i", KudosContextHolder.get().tenantId)
        }
    }

    /**
     * Test implementation that records how many times the provider-context extension point is invoked.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingProviderContextProcess : IFeignProviderContextProcess {
        var calls = 0

        override fun processContext(request: HttpServletRequest, context: KudosContext) {
            calls++
            context.subSystemCode = "from-processor"
        }
    }

    /**
     * Test implementation that always throws, to verify SPI failures are isolated.
     *
     * @author K
     * @since 1.0.0
     */
    private class ThrowingProviderContextProcess : IFeignProviderContextProcess {
        override fun processContext(request: HttpServletRequest, context: KudosContext) {
            throw IllegalStateException("boom from test processor")
        }
    }

}
